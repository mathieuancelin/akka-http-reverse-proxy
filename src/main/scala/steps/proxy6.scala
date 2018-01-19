package steps

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Authority
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Host, RawHeader}
import akka.pattern.CircuitBreaker
import akka.stream.ActorMaterializer
import akka.util.ByteString
import io.circe._
import io.circe.parser._
import models._
import util.Retry

import scala.concurrent.duration._
import scala.concurrent.{Future, TimeoutException}

// Live changes
class ReverseProxyV6 {

  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val http = Http(system)

  val AbsoluteUri = """(?is)^(https?)://([^/]+)(/.*|$)""".r

  val counter = new AtomicInteger(0)

  val circuitBreakers = new ConcurrentHashMap[String, CircuitBreaker]()

  def BadRequest(message: String) = HttpResponse(
    400,
    entity = HttpEntity(ContentTypes.`application/json`, Json.obj("error" -> Json.fromString(message)).noSpaces)
  )

  def NotFound(path: String) = HttpResponse(
    404,
    entity = HttpEntity(ContentTypes.`application/json`, Json.obj("error" -> Json.fromString(s"$path not found")).noSpaces)
  )

  def GatewayTimeout() = HttpResponse(
    504,
    entity = HttpEntity(ContentTypes.`application/json`, Json.obj("error" -> Json.fromString(s"Target servers timeout")).noSpaces)
  )

  def BadGateway(message: String) = HttpResponse(
    502,
    entity = HttpEntity(ContentTypes.`application/json`, Json.obj("error" -> Json.fromString(message)).noSpaces)
  )

  val servicesRef: AtomicReference[Map[String, Seq[Target]]] = new AtomicReference[Map[String, Seq[Target]]](Map(
    "test.foo.bar" -> Seq(
      Target.weighted("http://127.0.0.1:8081", 1),
      Target.weighted("http://127.0.0.1:8082", 1),
      Target.weighted("http://127.0.0.1:8083", 1)
    )
  ))

  def extractHost(request: HttpRequest): String = request.uri.toString() match {
    case AbsoluteUri(_, hostPort, _) => hostPort
    case _                           => request.header[Host].map(_.host.address()).getOrElse("--")
  }

  def handler(request: HttpRequest): Future[HttpResponse] = {
    val host = extractHost(request)
    servicesRef.get().get(host) match {
      case Some(rawSeq) => {
        val seq = rawSeq.flatMap(t => (1 to t.weight).map(_ => t))
        Retry.retry(3) {
          val index = counter.incrementAndGet() % (if (seq.nonEmpty) seq.size else 1)
          val target = seq.apply(index)
          val circuitBreaker = circuitBreakers.computeIfAbsent(target.url, _ => new CircuitBreaker(
            system.scheduler,
            maxFailures = 5,
            callTimeout = 30.seconds,
            resetTimeout = 10.seconds))
          val headersIn: Seq[HttpHeader] =
            request.headers.filterNot(t => t.name() == "Host") :+
              Host(target.host) :+
              RawHeader("X-Fowarded-Host", host) :+
              RawHeader("X-Fowarded-Scheme", request.uri.scheme)
          val proxyRequest = request.copy(
            uri = request.uri.copy(
              scheme = target.scheme,
              authority = Authority(host = Uri.NamedHost(target.host), port = target.port)
            ),
            headers = headersIn.toList,
            protocol = target.protocol
          )
          circuitBreaker.withCircuitBreaker(http.singleRequest(proxyRequest))
        }.recover {
          case _: akka.pattern.CircuitBreakerOpenException => BadGateway("Circuit breaker opened")
          case _: TimeoutException => GatewayTimeout()
          case e => BadGateway(e.getMessage)
        }
      }
      case None => Future.successful(NotFound(host))
    }
  }

  def apiHandler(request: HttpRequest): Future[HttpResponse] = {
    request.entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map(_.utf8String).map { body =>
      parse(body) match {
        case Left(e) => BadRequest(e.message)
        case Right(json) => json.as(Command.decoder) match {
          case Left(e) => BadRequest(e.message)
          case Right(Command("ADD", domain, target)) =>
            servicesRef.getAndUpdate { services =>
              services.get(domain) match {
                case Some(seq) => services + (domain -> (seq :+ Target(target)))
                case None => services + (domain -> Seq(Target(target)))
              }
            }
            HttpResponse(
              200,
              entity = HttpEntity(ContentTypes.`application/json`, Json.obj("done" -> Json.fromBoolean(true)).noSpaces)
            )
          case Right(Command("REM", domain, target)) =>
            servicesRef.getAndUpdate { services =>
              services.get(domain) match {
                case Some(seq) if seq.size == 1 && seq(0).url == target => services - domain
                case Some(seq) => services + (domain -> seq.filterNot(_ == Target(target)))
                case _ => services
              }
            }
            HttpResponse(
              200,
              entity = HttpEntity(ContentTypes.`application/json`, Json.obj("done" -> Json.fromBoolean(true)).noSpaces)
            )
        }
      }
    }
  }

  def start(host: String, port: Int) {
    http.bindAndHandleAsync(handler, host, port)
    http.bindAndHandleAsync(apiHandler, host, port + 1)
  }
}

