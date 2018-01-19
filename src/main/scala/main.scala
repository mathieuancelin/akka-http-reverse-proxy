import steps._

object Main {
  def main(args: Array[String]) {
    println("\nListening on http://0.0.0.0:8080 ...\n")
    new ReverseProxyV6().start("0.0.0.0", 8080)
  }
}