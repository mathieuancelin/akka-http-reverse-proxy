package models

import akka.http.scaladsl.model.{HttpProtocol, HttpProtocols}

case class Target(scheme: String, host: String, port: Int, weight: Int = 1, protocol: HttpProtocol = HttpProtocols.`HTTP/1.1`) {
  def url: String = s"$scheme://$host:$port"
}

object Target {
  def apply(url: String): Target = {
    val scheme = url.split("://")(0)
    val host = url.split("://")(1).split(":")(0)
    val port = url.split("://")(1).split(":")(1).toInt
    Target(scheme, host, port)
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
