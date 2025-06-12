package pcd.ass03.actor

import akka.actor.typed.*
import akka.actor.typed.scaladsl.*
import akka.cluster.ClusterEvent
import akka.cluster.ClusterEvent.*
import akka.cluster.ddata.typed.scaladsl.DistributedData
import akka.cluster.typed.*
import com.typesafe.config.Config
import pcd.ass03.actor.{FoodManager, PlayerRegistry, WorldManager}

object GameClusterSupervisor:
  def apply(): Behavior[Nothing] =
    Behaviors.setup[Nothing]: context =>
      val cluster = Cluster(context.system)
      
      val worldManager = context.spawn(
        Behaviors.supervise(WorldManager(context.system.settings.config))
          .onFailure[Exception](SupervisorStrategy.restart), "WorldManagerSupervisor")
      
      cluster.subscriptions ! Subscribe(
        context.messageAdapter[MemberEvent](ClusterEvent),
        classOf[MemberEvent]
      )
      
      Behaviors.receiveMessage:
        case ClusterEvent(MemberRemoved(member, _)) =>
          Behaviors.same
          
      val worldManagerProxy = ClusterSingleton(context.system).init(
        SingletonActor(WorldManager(context.system.settings.config), "WorldManager"))
        
      val foodManagerProxy = ClusterSingleton(context.system).init( 
        SingletonActor(FoodManager(context.system.settings.config), "FoodManager"))
        
      context.spawn(PlayerRegistry(), "PlayerRegistry")
      
      Behaviors.empty
