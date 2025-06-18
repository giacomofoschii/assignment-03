package pcd.ass03.distributed

import akka.cluster.typed._

import pcd.ass03.actors.{GameClusterSupervisorActor, PlayerActor, WorldManagerActor}
import pcd.ass03.startupWithRole

object AIClient:
  def main(args: Array[String]): Unit =
    val numAI = args.headOption.map(_.toInt).getOrElse(4)

    val system = startupWithRole("ai-client", 0)(GameClusterSupervisorActor())

    val worldManagerProxy = ClusterSingleton(system).init(
      SingletonActor(WorldManagerActor(system.settings.config), "WorldManager")
    )
    
    Thread.sleep(3000) // Wait for the WorldManager to be ready

    (1 to numAI).foreach: i =>
      system.systemActorOf(PlayerActor(s"AI-$i", worldManagerProxy, isAI = true), s"AI-Player-$i")

    // Shutdown hook to gracefully terminate the system
    sys.addShutdownHook {
      system.terminate()
    }

    Thread.currentThread().join()