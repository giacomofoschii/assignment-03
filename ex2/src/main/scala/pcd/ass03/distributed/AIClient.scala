package pcd.ass03.distributed

import akka.actor.typed._
import akka.cluster.typed._
import com.typesafe.config.ConfigFactory

import pcd.ass03.actor.{GameClusterSupervisor, PlayerActor, WorldManager}

object AIClient extends App:
  val numAI = args.headOption.map(_.toInt).getOrElse(4)
  
  val config = ConfigFactory
    .parseString(s"""
      akka.remote.artery.canonical.port=0
      akka.cluster.roles = [ai-client]
      """)
    .withFallback(ConfigFactory.load("agario"))
  
  val system = ActorSystem(GameClusterSupervisor(), "agario", config)
  
  val worldManager = ClusterSingleton(system).init(
    SingletonActor(WorldManager(config), "WorldManager")
  )

  (1 to numAI).foreach: i =>
    system.systemActorOf(PlayerActor(s"AI-$i", worldManager, isAI = true), s"AI-Player-$i")
    
  println(s"Spawned $numAI AI players")
  Thread.sleep(Long.MaxValue)