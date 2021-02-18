name := """church-songs"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala,JavaAppPackaging).settings(
  dockerSettings
)

scalaVersion := "2.13.3"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.42"
libraryDependencies += jdbc
libraryDependencies += "org.playframework.anorm" %% "anorm" % "2.6.4"
libraryDependencies += "biz.paluch.logging" % "logstash-gelf" % "1.14.0"
libraryDependencies += "ai.x" %% "play-json-extensions" % "0.40.2"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"

javaOptions in Universal ++= Seq(
  "-Dpidfile.path=/dev/null"
)

lazy val dockerSettings =
  Seq(
    daemonUser.in(Docker) := "root",
    maintainer.in(Docker) := "Rowan Pillay",
    version.in(Docker) := "latest",
    dockerBaseImage := "openjdk:8",
    dockerExposedPorts := Vector(9000),
    dockerRepository := Some("docker.io"),
    dockerUsername := Some("churchsource")
  )