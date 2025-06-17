package pcd.ass03.distributed

import akka.cluster.typed._
import pcd.ass03.GameConfig.FiveSeconds
import pcd.ass03.actors.{GameClusterSupervisorActor, PlayerActor, WorldManagerActor}
import pcd.ass03.startupWithRole

import scala.concurrent.{Await, TimeoutException}
import scala.io.StdIn

object GameClient:
  def main(args: Array[String]): Unit =
    val playerName = if args.nonEmpty then args(0) else {
      println("Enter player name:")
      StdIn.readLine()
    }
  
    val system = startupWithRole("client", 0)(GameClusterSupervisorActor())

    val worldManagerProxy = ClusterSingleton(system).init(
      SingletonActor(WorldManagerActor(system.settings.config), "WorldManager")
    )
  
    val playerActor = system.systemActorOf(
      PlayerActor(playerName, worldManagerProxy),
      s"Player-$playerName"
    )
  
    println(s"Player $playerName connected to the game server")
    println("Use mouse to move around")
    println("Press Enter to exit...")

    try {
      StdIn.readLine()
    } catch {
      case _: Exception =>
        println("Input interrupted, shutting down...")
    }
  
    println("Terminating client...")

    try {
      system.terminate()
    } catch {
      case _: TimeoutException =>
        println("Timeout during system termination")
    }