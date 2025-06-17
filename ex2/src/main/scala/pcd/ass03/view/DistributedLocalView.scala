package pcd.ass03.view

import akka.actor.typed.ActorRef

import java.awt.Graphics2D
import scala.swing._
import scala.swing.Dialog._
import javax.swing.SwingUtilities

import pcd.ass03.distributed.{MoveDirection, PlayerActorMessage, Respawn}
import pcd.ass03.model.World
import pcd.ass03.GameConfig._

class DistributedLocalView(playerId: String) extends MainFrame:
  private var world: World = World(WorldSize, WorldSize, Seq.empty, Seq.empty)
  private var playerActor: Option[ActorRef[PlayerActorMessage]] = None
  private var isActive = true

  title = s"Agar.io - Local View ($playerId)"
  preferredSize = new Dimension(WindowSize, WindowSize)

  contents = new Panel:
    listenTo(mouse.moves)
    focusable = true
    requestFocusInWindow()

    override def paintComponent(g: Graphics2D): Unit =
      if isActive then
        val playerOpt = world.players.find(_.id == playerId)
        val (offsetX, offsetY) = playerOpt
          .map(p => (p.x - size.width / 2.0, p.y - size.height / 2.0))
          .getOrElse((0.0, 0.0))
        AgarViewUtils.drawWorld(g, world, offsetX, offsetY)
      else
        g.setColor(java.awt.Color.DARK_GRAY)
        g.fillRect(0, 0, size.width, size.height)
        g.setColor(java.awt.Color.WHITE)
        g.drawString("Waiting for respawn...", size.width / 2 - 60, size.height / 2)

    reactions += { case e: event.MouseMoved =>
      if isActive then
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

  def setActive(active: Boolean): Unit =
    isActive = active
    repaint()

  def showRespawnDialog(): Unit =
    SwingUtilities.invokeLater(() => {
      isActive = false
      repaint()
      
      val result = showConfirmation(
        this,
        "\nDo you want to respawn?",
        "Game Over",
        Options.YesNo,
        Message.Question
      )
      
      result match
        case Result.Yes =>
          playerActor.foreach(_ ! Respawn)
        case _ =>
          dispose()
          System.exit(0)
    })

  override def dispose(): Unit =
    isActive = false
    playerActor = None
    super.dispose()