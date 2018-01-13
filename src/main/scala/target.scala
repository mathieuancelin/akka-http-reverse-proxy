package models

case class Target(scheme: String, host: String, port: Int)

object Target {
  def apply(url: String): Target = {
    val scheme = url.split("://")(0)
    val host = url.split("://")(1).split(":")(0)
    val port = url.split("://")(1).split(":")(1).toInt
    Target(scheme, host, port)
  }
} 