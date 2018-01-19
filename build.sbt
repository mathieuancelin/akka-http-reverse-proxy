enablePlugins(JavaAppPackaging)
enablePlugins(JavaAgent)

name         := """akka-http-reverse-proxy"""
organization := "io.mancelin"
version      := "0.1.0"
scalaVersion := "2.12.4"

libraryDependencies ++= {
  lazy val akkaHttpVersion = "10.0.11"
  lazy val akkaVersion     = "2.5.9"
  Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka"  %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-http2-support" % "10.1.0-RC1",
    "io.circe" %% "circe-core" % "0.9.0",
    "io.circe" %% "circe-generic" % "0.9.0",
    "io.circe" %% "circe-parser" % "0.9.0",
    "io.circe" %% "circe-optics" % "0.9.0"
  )
}

javaAgents += "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.6" % "runtime"

sources in (Compile, doc) := Seq.empty
publishArtifact in (Compile, packageDoc) := false

mainClass in Compile := Some("Main")
mainClass in reStart := Some("Main")
mainClass in assembly := Some("Main")

assemblyJarName in assembly := "reverseproxy.jar"
test in assembly := {}