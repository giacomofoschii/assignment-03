package pcd.ass03.distributed

import akka.actor.typed._
import akka.cluster.typed._
import com.typesafe.config.ConfigFactory

import pcd.ass03.actor.{GameClusterSupervisor, PlayerActor, WorldManager}
import scala.io.StdIn

object GameClient:
  def main(args: Array[String]): Unit =
    val playerName = if args.nonEmpty then args(0) else {
      println("Enter player name:")
      StdIn.readLine()
    }
  
    val config = ConfigFactory
      .parseString(
        s"""
          akka.remote.artery.canonical.port=0
          akka.cluster.roles = [client]
          """)
      .withFallback(ConfigFactory.load("agario"))
  
    val system = ActorSystem(GameClusterSupervisor(), "agario", config)
  
    Thread.sleep(3000)
    
    val worldManagerProxy = ClusterSingleton(system).init(
      SingletonActor(WorldManager(config), "WorldManager")
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
