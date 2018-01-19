package models

import akka.http.scaladsl.model.{HttpProtocol, HttpProtocols}
import io.circe.Decoder
import io.circe.generic.semiauto._

case class Target(scheme: String, host: String, port: Int, weight: Int = 1, protocol: HttpProtocol = HttpProtocols.`HTTP/1.1`) {
  def url: String = s"$scheme://$host:$port"
}

object Target {
  def apply(url: String): Target = {
    url.split("://|:").toList match {
      case scheme :: host :: port :: Nil => Target(scheme, host, port.toInt)
      case _ => throw new RuntimeException(s"Bad target: $url")
    }
  }

  def weighted(url: String, weight: Int): Target = {
    Target(url).copy(weight = weight)
  }

  def proto(url: String, protocol: HttpProtocol): Target = {
    Target(url).copy(protocol = protocol)
  }

  def protoWeight(url: String, weight: Int, protocol: HttpProtocol): Target = {
    Target(url).copy(weight = weight, protocol = protocol)
  }
}

case class Command(action: String, domain: String, target: String)

object Command {
  val decoder: Decoder[Command] = deriveDecoder[Command]
}
