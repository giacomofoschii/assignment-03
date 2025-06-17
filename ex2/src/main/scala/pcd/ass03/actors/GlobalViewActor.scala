package pcd.ass03.actors

import akka.actor.typed.*
import akka.actor.typed.scaladsl.*
import akka.util.Timeout
import pcd.ass03.distributed.*
import pcd.ass03.model.World
import pcd.ass03.view.GlobalViewFrame
import pcd.ass03.GameConfig._

import scala.util.{Failure, Success}

object GlobalViewActor:
  trait Command

  private case class UpdateWorld(world: World) extends Command

  private case object RequestUpdate extends Command

  def apply(worldManager: ActorRef[WorldManagerMessage]): Behavior[Command] =
    Behaviors.setup: context =>
      val view = new GlobalViewFrame()

      Behaviors.withTimers: timers =>
        timers.startTimerAtFixedRate(RequestUpdate, ThirtyMillis)

        implicit val timeout: Timeout = Timeout(ThreeSeconds)

        Behaviors.receiveMessage:
          case UpdateWorld(world) =>
            view.updateWorld(world)
            Behaviors.same

          case RequestUpdate =>
            context.ask(worldManager, GetWorldState.apply):
              case Success(WorldState(world)) =>
                UpdateWorld(world)
              case Failure(_) =>
                UpdateWorld(World(WorldWidth, WorldHeight, Seq.empty, Seq.empty))
            Behaviors.same
