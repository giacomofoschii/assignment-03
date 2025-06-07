
ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.6"

lazy val root = (project in file("."))
  .settings(
    name := "ex2"
  )

resolvers += "Akka library repository".at("https://repo.akka.io/maven")

lazy val akkaVersion = "2.10.5"
lazy val akkaGroup = "com.typesafe.akka"
libraryDependencies ++= Seq(
  akkaGroup %% "akka-actor-typed" % "2.10.5",
  akkaGroup %% "akka-actor" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.5.6",
  akkaGroup %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.19" % Test,
  "org.scala-lang.modules" %% "scala-swing" % "3.0.0"
)