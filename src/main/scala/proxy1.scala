import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri, ContentTypes, HttpEntity, HttpHeader, ContentType, HttpProtocols}
import akka.http.scaladsl.model.headers.{`Raw-Request-URI`, `Remote-Address`, `Content-Type`, Host, RawHeader}
import akka.http.scaladsl.model.Uri.Authority
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.config.ConfigFactory
import play.api.libs.json.Json

import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicLong}

import scala.concurrent.{ExecutionContextExecutor, Future}

import models._

class ReverseProxyV1 {

  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val http = Http(system)

  val NotFound = HttpResponse(
    404,
    entity = HttpEntity(ContentTypes.`application/json`, Json.stringify(Json.obj("error" -> "Not found")))
  )

  val services: Map[String, Target] = Map(
    "test.foo.bar" -> Target("http://127.0.0.1:8081")
  )

  def extractHost(request: HttpRequest): String = request.header[Host].map(_.host.address()).getOrElse("--")

  def handler(request: HttpRequest): Future[HttpResponse] = {
    val host = extractHost(request)
    services.get(host) match {
      case Some(target) => {
        val inCtype: ContentType = request
          .header[`Content-Type`]
          .map(_.contentType)
          .getOrElse(ContentTypes.`text/plain(UTF-8)`)
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
      case None => Future.successful(NotFound)
    }        
  }

  def start(host: String, port: Int) {
    http.bindAndHandleAsync(handler, host, port)
  }
}
