import java.net.URI

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri, ContentTypes, HttpEntity, HttpHeader, ContentType, HttpProtocols}
import akka.http.scaladsl.model.headers.{`Raw-Request-URI`, `Remote-Address`, `Content-Type`, Host}
import akka.http.scaladsl.model.Uri.Authority
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.config.ConfigFactory
import play.api.libs.json.Json

import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicLong}

import scala.concurrent.{ExecutionContextExecutor, Future}

case class Target(scheme: String, host: String, port: Int)

object Target {
  def apply(url: String): Target = {
    val scheme = url.split("://")(0)
    val host = url.split("://")(1).split(":")(0)
    val port = url.split("://")(1).split(":")(1).toInt
    Target(scheme, host, port)
  }
} 

object ReverseProxy {

  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val http = Http(system)

  val config = ConfigFactory.load()
  val useFlow = if (config.hasPath("useFlow")) config.getBoolean("useFlow") else false
  val counter = new AtomicInteger(0)
  val AbsoluteUri = """(?is)^(https?)://([^/]+)(/.*|$)""".r

  val services: Map[String, Seq[Target]] = Map(
    "test.foo.bar" -> Seq(
      Target("http://127.0.0.1:8081"),
      Target("http://127.0.0.1:8082"),
      Target("http://127.0.0.1:8083")
    )
  )

  def flow(host: String, port: Int): Flow[HttpRequest, HttpResponse, Any] = http.outgoingConnection(host, port)

  def uriString(request: HttpRequest): String = request.header[`Raw-Request-URI`].map(_.uri).getOrElse(request.uri.toRelative.toString())

  def host(request: HttpRequest): String = {
    uriString(request) match {
      case AbsoluteUri(_, hostPort, _) => hostPort
      case _                           => request.header[Host].map(_.host.address()).getOrElse("")
    }
  }

  def handler(request: HttpRequest): Future[HttpResponse] = {
    services.get(host(request)) match {
      case Some(seq) => {
        val index = counter.incrementAndGet() % (if (seq.nonEmpty) seq.size else 1)
        val target = seq.apply(index)
        val inCtype: ContentType = request
          .header[`Content-Type`]
          .map(_.contentType)
          .getOrElse(ContentTypes.`application/octet-stream`)
        val headersIn: Seq[HttpHeader] = request.headers.filterNot(t => t.name() == "Host" || t.name() == "Timeout-Access") :+ Host(target.host)
        val proxyRequest = HttpRequest(
            method = request.method,
            uri = Uri(
              scheme = target.scheme,
              authority = Authority(host = Uri.NamedHost(target.host), port = target.port),
              path = request.uri.toRelative.path,
              queryString = request.uri.toRelative.rawQueryString,
              fragment = request.uri.toRelative.fragment
            ),
            headers = headersIn.toList,
            entity = HttpEntity(inCtype, request.entity.dataBytes),
            protocol = HttpProtocols.`HTTP/1.1`
        )
        if (useFlow) {
          Source.single(proxyRequest).via(flow(target.host, target.port)).runWith(Sink.head)
        } else {
          http.singleRequest(proxyRequest)
        }
      }
      case None => Future.successful {
        HttpResponse(
          404,
          entity = HttpEntity(ContentTypes.`application/json`, Json.stringify(Json.obj("error" -> "Not found")))
        )
      }
    }        
  }

  def main(args: Array[String]) {
    http.bindAndHandleAsync(handler, "0.0.0.0", 8080)
  }
}
