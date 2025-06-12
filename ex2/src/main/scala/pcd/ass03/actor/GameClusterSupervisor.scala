package pcd.ass03.actor

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.cluster.typed._
import akka.cluster.ClusterEvent._
import akka.cluster.typed.{Cluster, Subscribe}

object GameClusterSupervisor:
  trait ClusterCommand
  private case class MemberEventWrapper(event: MemberEvent) extends ClusterCommand

  def apply(): Behavior[Nothing] =
    Behaviors.setup[ClusterCommand]: context =>
      val cluster = Cluster(context.system)

      ClusterSingleton(context.system).init(
        SingletonActor(WorldManager(context.system.settings.config), "WorldManager")
      )

      ClusterSingleton(context.system).init(
        SingletonActor(
          FoodManager(context.system.settings.config),
          "FoodManager"
        )
      )

      context.spawn(PlayerRegistry(), "PlayerRegistry")

      val memberEventAdapter =  context.messageAdapter[MemberEvent](MemberEventWrapper.apply)
      
      cluster.subscriptions ! Subscribe(memberEventAdapter, classOf[MemberEvent])
      
      Behaviors.receiveMessage:
        case MemberEventWrapper(MemberUp(member)) =>
          context.log.info(s"Member is Up: ${member.address}")
          Behaviors.same
        case MemberEventWrapper(MemberRemoved(member, previousStatus)) =>
          context.log.info(s"Member is Removed: ${member.address} after $previousStatus")
          Behaviors.same
        case MemberEventWrapper(_) =>
          Behaviors.same
    .narrow
