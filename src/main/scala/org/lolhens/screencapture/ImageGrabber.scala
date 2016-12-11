package org.lolhens.screencapture

import java.awt.image.BufferedImage
import java.awt.{GraphicsDevice, Rectangle, Robot}

import swave.core.Spout

/**
  * Created by pierr on 11.12.2016.
  */
object ImageGrabber {
  def apply(graphicsDevice: GraphicsDevice): Spout[BufferedImage] = {
    val robot = new Robot(graphicsDevice)
    val screenSize = new Rectangle(graphicsDevice.getDisplayMode.getWidth, graphicsDevice.getDisplayMode.getHeight)

    Spout.continually(robot.createScreenCapture(screenSize))
  }
}
