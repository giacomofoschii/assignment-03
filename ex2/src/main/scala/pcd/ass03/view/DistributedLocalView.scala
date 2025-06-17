package pcd.ass03.view

import akka.actor.typed.ActorRef

import java.awt.Graphics2D
import scala.swing._

import pcd.ass03.distributed.{MoveDirection, PlayerActorMessage}
import pcd.ass03.model.World
import pcd.ass03.GameConfig._

class DistributedLocalView(playerId: String) extends MainFrame:
  private var world: World = World(WorldSize, WorldSize, Seq.empty, Seq.empty)
  private var playerActor: Option[ActorRef[PlayerActorMessage]] = None

  title = s"Agar.io - Local View ($playerId)"
  preferredSize = new Dimension(WindowSize, WindowSize)

  contents = new Panel:
    listenTo(mouse.moves)
    focusable = true
    requestFocusInWindow()

    override def paintComponent(g: Graphics2D): Unit =
      val playerOpt = world.players.find(_.id == playerId)
      val (offsetX, offsetY) = playerOpt
        .map(p => (p.x - size.width / 2.0, p.y - size.height / 2.0))
        .getOrElse((0.0, 0.0))
      AgarViewUtils.drawWorld(g, world, offsetX, offsetY)

    reactions += {case e: event.MouseMoved =>
      playerActor.foreach: actor =>
        val dx = (e.point.x - size.width / 2.0) * 0.01
        val dy = (e.point.y - size.height / 2.0) * 0.01
        actor ! MoveDirection(dx, dy)
    }

  open()

  def updateWorld(world: World): Unit =
    this.world = world
    repaint()

  def setPlayerActor(actor: ActorRef[PlayerActorMessage]): Unit =
    playerActor = Some(actor)