package pcd.ass03.actor

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.cluster.typed._
import akka.cluster.ClusterEvent._
import akka.cluster.typed.{Cluster, Subscribe}

import scala.concurrent.duration._

object GameClusterSupervisor:
  trait ClusterCommand
  private case class MemberEventWrapper(event: MemberEvent) extends ClusterCommand

  def apply(): Behavior[Nothing] =
    Behaviors
      .supervise(clusterBehavior())
      .onFailure(SupervisorStrategy.restart.withLimit(maxNrOfRetries = 10, withinTimeRange = 30.seconds))
      .narrow

  private def clusterBehavior(): Behavior[ClusterCommand] =
    Behaviors.setup[ClusterCommand]: context =>
      val cluster = Cluster(context.system)

      ClusterSingleton(context.system).init(
        SingletonActor(
          Behaviors
            .supervise(WorldManager(context.system.settings.config))
            .onFailure[Exception](SupervisorStrategy.restart),
          "WorldManager")
      )
      
      ClusterSingleton(context.system).init(
        SingletonActor(
          Behaviors
            .supervise(FoodManager(context.system.settings.config))
            .onFailure[Exception](SupervisorStrategy.restart),
          "FoodManager"
        )
      )

      context.spawn(
        Behaviors
          .supervise(PlayerRegistry())
          .onFailure[Exception](SupervisorStrategy.restart.withLimit(3, 10.seconds)),
        "PlayerRegistry"
      )

      val memberEventAdapter =  context.messageAdapter[MemberEvent](MemberEventWrapper.apply)
      
      cluster.subscriptions ! Subscribe(memberEventAdapter, classOf[MemberEvent])
      
      Behaviors.receiveMessage:
        case MemberEventWrapper(MemberUp(member)) =>
          context.log.info(s"Member is Up: ${member.address}")
          Behaviors.same

        case MemberEventWrapper(MemberRemoved(member, previousStatus)) =>
          context.log.info(s"Member is Removed: ${member.address} after $previousStatus")
          Behaviors.same

        case MemberEventWrapper(UnreachableMember(member)) =>
          context.log.warn(s"Member detected as unreachable: ${member.address}")
          Behaviors.same

        case MemberEventWrapper(ReachableMember(member)) =>
          context.log.info(s"Member is reachable again: ${member.address}")
          Behaviors.same

        case MemberEventWrapper(_) =>
          Behaviors.same