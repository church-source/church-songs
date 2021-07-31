name := "church-songs"
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala,JavaAppPackaging)
  .settings(
  dockerSettings
//  ,javaOptions in Universal ++= jvmSettings
  )

scalaVersion := "2.12.6"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.42"
libraryDependencies += jdbc
libraryDependencies += "org.playframework.anorm" %% "anorm" % "2.6.4"
libraryDependencies += "biz.paluch.logging" % "logstash-gelf" % "1.14.0"
libraryDependencies += "ai.x" %% "play-json-extensions" % "0.40.2"

libraryDependencies ++= Seq(
  "com.pauldijou" %% "jwt-play" % "0.19.0",
  "com.pauldijou" %% "jwt-core" % "0.19.0",
  "com.auth0" % "jwks-rsa" % "0.6.1"
)

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

lazy val jvmSettings = Seq(
  "-Dpidfile.path=/dev/null",
  "-J-Xtune:virtualized",
  "-J-XX:+UseContainerSupport",
  "-J-XX:InitialRAMPercentage=30",
  "-J-XX:MaxRAMPercentage=75",
  "-J-Xjit:enableSelfTuningScratchMemoryUsageBeforeCompile",
  "-J-XX:ActiveProcessorCount=4",
  "-J-Xss128k",
  "-XX:MaxRAM=72m",
  "-XX:+UseSerialGC",
  "-J-XX:+ClassRelationshipVerifier",
  "-J-Xcompressedrefs"
)