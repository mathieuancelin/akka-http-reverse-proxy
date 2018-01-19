package steps

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Authority
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Host, RawHeader}
import akka.stream.ActorMaterializer
import io.circe._
import models._

import scala.concurrent.Future

// Forward headers. Change host for virtual host targets
class ReverseProxyV1 {

  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val http = Http(system)

  def NotFound(path: String) = HttpResponse(
    404,
    entity = HttpEntity(ContentTypes.`application/json`, Json.obj("error" -> Json.fromString(s"$path not found")).noSpaces)
  )

  val services: Map[String, Target] = Map(
    "test.foo.bar" -> Target("http://127.0.0.1:8081")
  )

  def extractHost(request: HttpRequest): String = request.header[Host].map(_.host.address()).getOrElse("--")

  def handler(request: HttpRequest): Future[HttpResponse] = {
    val host = extractHost(request)
    services.get(host) match {
      case Some(target) => {
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
          headers = headersIn.toList
        )
        http.singleRequest(proxyRequest)
      }
      case None => Future.successful(NotFound(host))
    }        
  }

  def start(host: String, port: Int) {
    http.bindAndHandleAsync(handler, host, port)
  }
}
