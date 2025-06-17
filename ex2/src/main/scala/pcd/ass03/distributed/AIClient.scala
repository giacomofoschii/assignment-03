package pcd.ass03.distributed

import akka.cluster.typed._

import pcd.ass03.actors.{GameClusterSupervisor, PlayerActor, WorldManager}
import pcd.ass03.startupWithRole

object AIClient:
  def main(args: Array[String]): Unit =
    val numAI = args.headOption.map(_.toInt).getOrElse(4)

    val system = startupWithRole("ai-client", 0)(GameClusterSupervisor())

    Thread.sleep(3000)

    val worldManagerProxy = ClusterSingleton(system).init(
      SingletonActor(WorldManager(system.settings.config), "WorldManager")
    )

    (1 to numAI).foreach: i =>
      system.systemActorOf(PlayerActor(s"AI-$i", worldManagerProxy, isAI = true), s"AI-Player-$i")

    println(s"Spawned $numAI AI players")

    // Shutdown hook to gracefully terminate the system
    sys.addShutdownHook {
      system.terminate()
    }

    Thread.currentThread().join()