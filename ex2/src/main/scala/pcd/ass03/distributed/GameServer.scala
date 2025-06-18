package pcd.ass03.distributed

import akka.cluster.typed._

import pcd.ass03.actors.{GameClusterSupervisorActor, GlobalViewActor, WorldManagerActor}
import pcd.ass03.startupWithRole

object GameServer extends App:
  private val portNum = 25251
  val system = startupWithRole("server", portNum)(GameClusterSupervisorActor())

  val worldManagerProxy = ClusterSingleton(system).init(
    SingletonActor(WorldManagerActor(system.settings.config), "WorldManager")
  )

  system.systemActorOf(GlobalViewActor(worldManagerProxy), "GlobalView")

  println(s"Game server started on port $portNum")

  // Shutdown hook to gracefully terminate the actor system
  sys.addShutdownHook {
    println("Shutting down game server...")
    system.terminate()
  }

