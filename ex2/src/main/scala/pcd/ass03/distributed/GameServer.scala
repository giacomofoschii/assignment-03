package pcd.ass03.distributed

import akka.actor.typed._
import akka.cluster.typed._
import com.typesafe.config.ConfigFactory

import pcd.ass03.actor.{GameClusterSupervisor, WorldManager}
import pcd.ass03.view.GlobalViewActor

object GameServer extends App:
  val config = ConfigFactory
    .parseString(s"""
      akka.remote.artery.canonical.port=25251
      akka.cluster.roles = [server]
      """)
    .withFallback(ConfigFactory.load("agario"))

  val system = ActorSystem(GameClusterSupervisor(), "agario", config)

  Thread.sleep(2000)
  
  val worldManagerProxy = ClusterSingleton(system).init(
    SingletonActor(WorldManager(config), "WorldManager")
  )

  system.systemActorOf(GlobalViewActor(worldManagerProxy), "GlobalView")

  println("Game server started on port 25251")
  println("Global view opened")

  // Shutdown hook to gracefully terminate the actor system
  sys.addShutdownHook {
    println("Shutting down game server...")
    system.terminate()
  }

