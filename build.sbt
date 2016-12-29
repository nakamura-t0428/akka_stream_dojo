lazy val commonSettings = Seq(
  organization := "ss",
  version := "1.0",
  scalaVersion := "2.11.8"
)

lazy val akkaVersion = "2.3.9"
lazy val sprayVersion = "1.3.3"

lazy val libAkka = Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.4.14",
    "com.typesafe.akka" %% "akka-agent" % "2.4.14",
    "com.typesafe.akka" %% "akka-camel" % "2.4.14",
    "com.typesafe.akka" %% "akka-cluster" % "2.4.14",
    "com.typesafe.akka" %% "akka-osgi" % "2.4.14",
    "com.typesafe.akka" %% "akka-remote" % "2.4.14",
    "com.typesafe.akka" %% "akka-slf4j" % "2.4.14",
    "com.typesafe.akka" %% "akka-stream" % "2.4.14",
    "com.typesafe.akka" %% "akka-http" % "10.0.0",
    "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.0",
    "com.typesafe.akka" %% "akka-testkit" % "2.4.14" % Test
  )

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "akka_stream_dojo",
    libraryDependencies ++= libAkka
  )

