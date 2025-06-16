package pcd.ass03.actor

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.cluster.typed._
import akka.cluster.ClusterEvent._

import scala.concurrent.duration._

object GameClusterSupervisor:
  private trait ClusterCommand
  private case class MemberEventWrapper(event: MemberEvent) extends ClusterCommand
  private case class ReachabilityEventWrapper(event: ReachabilityEvent) extends ClusterCommand

  def apply(): Behavior[Nothing] =
    Behaviors
      .supervise(clusterBehavior())
      .onFailure(SupervisorStrategy.restart.withLimit(maxNrOfRetries = 10, withinTimeRange = 30.seconds))
      .narrow

  private def clusterBehavior(): Behavior[ClusterCommand] =
    Behaviors.setup[ClusterCommand]: context =>
      val cluster = Cluster(context.system)

      if cluster.selfMember.roles.contains("server") then
        println("Initializing cluster singletons on server node")
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
      else
        println("Client node initialized")

      context.spawn(
        Behaviors
          .supervise(PlayerRegistry())
          .onFailure[Exception](SupervisorStrategy.restart.withLimit(3, 10.seconds)),
        "PlayerRegistry"
      )

      val memberEventAdapter =  context.messageAdapter[MemberEvent](MemberEventWrapper.apply)
      val reachabilityEventAdapter = context.messageAdapter[ReachabilityEvent](ReachabilityEventWrapper.apply)

      cluster.subscriptions ! Subscribe(memberEventAdapter, classOf[MemberEvent])
      cluster.subscriptions ! Subscribe(reachabilityEventAdapter, classOf[ReachabilityEvent])

      Behaviors.receiveMessage:
        case MemberEventWrapper(MemberUp(member)) =>
          println(s"Member is Up: ${member.address}")
          Behaviors.same

        case MemberEventWrapper(MemberRemoved(member, previousStatus)) =>
          println(s"Member is Removed: ${member.address} after $previousStatus")
          Behaviors.same

        case MemberEventWrapper(_) =>
          Behaviors.same

        case ReachabilityEventWrapper(UnreachableMember(member)) =>
          println(s"Member detected as unreachable: ${member.address}")
          Behaviors.same

        case ReachabilityEventWrapper(ReachableMember(member)) =>
          println(s"Member is reachable again: ${member.address}")
          Behaviors.same
