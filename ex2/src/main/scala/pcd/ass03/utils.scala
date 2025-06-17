package pcd.ass03

import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

val seeds = List(2551, 2552)

object GameConfig:
  val TwoSeconds: FiniteDuration = 2.seconds
  val ThreeSeconds: FiniteDuration = 3.seconds
  val FiveSeconds : FiniteDuration = 5.seconds
  val ThirtyMillis: FiniteDuration = 30.millis
  val FiftyMillis: FiniteDuration = 50.millis
  val HundredMillis: FiniteDuration = 100.millis
  val DefaultSpeed = 10.0
  val WorldWidth = 1400
  val WorldHeight = 1400
  val MaxFood = 200
  val WindowSize = 800
  val PlayerMass = 120.0

def startupWithRole[X](role: String, port: Int, file: String = "agario")(root: => Behavior[X]): ActorSystem[X] =
  val config = ConfigFactory
    .parseString(s"""
      akka.remote.artery.canonical.port=$port
      akka.cluster.roles = [$role]
      """)
    .withFallback(ConfigFactory.load(file))

  // Create an Akka system
  ActorSystem(root, "agario", config)
