enablePlugins(JavaAppPackaging)

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
    "com.typesafe.play" %% "play-json" % "2.6.7", 
  )
}

sources in (Compile, doc) := Seq.empty
publishArtifact in (Compile, packageDoc) := false

mainClass in Compile := Some("ReverseProxy")
mainClass in reStart := Some("ReverseProxy")
mainClass in assembly := Some("ReverseProxy")

assemblyJarName in assembly := "reverse-proxy.jar"
test in assembly := {}