package pcd.ass03.view

import akka.actor.typed.*
import akka.actor.typed.scaladsl.*
import akka.util.Timeout

import scala.concurrent.duration.*
import scala.swing.*
import java.awt.Graphics2D
import scala.util.{Failure, Success}
import pcd.ass03.model.World
import pcd.ass03.distributed.*

class GlobalViewFrame extends MainFrame:
  private var world: World = World(1000, 1000, Seq.empty, Seq.empty)

  title = "Agar.io Global View"
  preferredSize = new Dimension(800, 800)

  contents = new Panel:
    override def paintComponent(g: Graphics2D): Unit =
      AgarViewUtils.drawWorld(g, world)

  open()

  def updateWorld(newWorld: World): Unit =
    world = newWorld
    repaint()

object GlobalViewActor:
  trait Command
  case class UpdateWorld(world: World) extends Command
  case object RequestUpdate extends Command

  def apply(worldManager: ActorRef[WorldManagerMessage]): Behavior[Command] =
    Behaviors.setup: context =>
      val view = new GlobalViewFrame()

      Behaviors.withTimers: timers =>
        timers.startTimerAtFixedRate(RequestUpdate, 30.millis)

        implicit val timeout: Timeout = Timeout(3.seconds)

        Behaviors.receiveMessage:
          case UpdateWorld(world) =>
            view.updateWorld(world)
            Behaviors.same

          case RequestUpdate =>
            context.ask(worldManager, GetWorldState.apply):
              case Success(WorldState(world)) =>
                UpdateWorld(world)
              case Failure(_) =>
                UpdateWorld(World(1000, 1000, Seq.empty, Seq.empty))
            Behaviors.same
            