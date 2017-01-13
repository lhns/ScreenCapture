package org.lolhens.screencapture

import java.awt.image.BufferedImage
import java.awt.{Component, Frame, Graphics, GraphicsDevice}
import javax.swing.JFrame

import swave.core.{Drain, DrainUtil}

/**
  * Created by pierr on 11.12.2016.
  */
object ImageCanvas {
  def apply(): Drain[BufferedImage, Component] = {
    @volatile var image: Option[BufferedImage] = None

    val component = new Component() {
      override def paint(graphics: Graphics): Unit = image.foreach { img =>
        graphics.drawImage(img, 0, 0, getWidth, getHeight, this)
      }
    }

    DrainUtil.foreachReturning[BufferedImage, Component] { img =>
      image = Some(img)
      component.repaint()
    }(component)
  }

  def canvas(graphicsDevice: GraphicsDevice, fullscreen: Boolean = false): JFrame = {
    val frame = new JFrame(graphicsDevice.getDefaultConfiguration)
    frame.setSize(800, 600)
    if (fullscreen) {
      frame.setExtendedState(Frame.MAXIMIZED_BOTH)
      frame.setUndecorated(true)
    }
    frame.setVisible(true)
    frame
  }

  def canvasDrain(graphicsDevice: GraphicsDevice, fullscreen: Boolean = false): Drain[BufferedImage, JFrame] = {
    apply().mapResult { component =>
      val frame = canvas(graphicsDevice, fullscreen)
      frame.add(component)
      frame
    }
  }
}
