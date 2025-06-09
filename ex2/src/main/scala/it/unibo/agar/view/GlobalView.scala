package it.unibo.agar.view

import it.unibo.agar.model.MockGameStateManager

import java.awt.{Color, Graphics2D}
import scala.swing.*

class GlobalView(manager: MockGameStateManager) extends MainFrame:

  title = "Agar.io - Global View"
  preferredSize = new Dimension(800, 800)

  contents = new Panel:
    override def paintComponent(g: Graphics2D): Unit =
      val world = manager.getWorld
      AgarViewUtils.drawWorld(g, world)
