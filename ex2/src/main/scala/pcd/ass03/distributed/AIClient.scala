package pcd.ass03.distributed

import akka.actor.typed._
import akka.cluster.typed._
import com.typesafe.config.ConfigFactory

import pcd.ass03.actor.{GameClusterSupervisor, PlayerActor, WorldManager}

object AIClient:
  def main(args: Array[String]): Unit =
    val numAI = args.headOption.map(_.toInt).getOrElse(4)

    val config = ConfigFactory
      .parseString(s"""
        akka.remote.artery.canonical.port=0
        akka.cluster.roles = [ai-client]
        """)
      .withFallback(ConfigFactory.load("agario"))

    val system = ActorSystem(GameClusterSupervisor(), "agario", config)

    Thread.sleep(5000)

    val worldManagerProxy = ClusterSingleton(system).init(
      SingletonActor(WorldManager(config), "WorldManager")
    )

    (1 to numAI).foreach: i =>
      system.systemActorOf(PlayerActor(s"AI-$i", worldManagerProxy, isAI = true), s"AI-Player-$i")
      Thread.sleep(500)

    println(s"Spawned $numAI AI players")

    // Shutdown hook to gracefully terminate the system
    sys.addShutdownHook {
      system.terminate()
    }

    Thread.currentThread().join()