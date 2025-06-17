package pcd.ass03.distributed

import akka.cluster.typed._

import pcd.ass03.actors.{GameClusterSupervisor, PlayerActor, WorldManager}
import pcd.ass03.startupWithRole
import scala.io.StdIn

object GameClient:
  def main(args: Array[String]): Unit =
    val playerName = if args.nonEmpty then args(0) else {
      println("Enter player name:")
      StdIn.readLine()
    }
  
    val system = startupWithRole("client", 0)(GameClusterSupervisor())
  
    Thread.sleep(3000)
    
    val worldManagerProxy = ClusterSingleton(system).init(
      SingletonActor(WorldManager(system.settings.config), "WorldManager")
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
    system.terminate()
