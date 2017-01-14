package org.lolhens.screencapture

import java.awt.image.BufferedImage
import java.awt.{GraphicsDevice, Robot}

import swave.core.Spout

/**
  * Created by pierr on 11.12.2016.
  */
object ImageGrabber {
  var logging = false

  def apply(graphicsDevice: GraphicsDevice): Spout[(BufferedImage, Long)] = {
    val robot = new Robot(graphicsDevice)
    val screenSize = graphicsDevice.getDefaultConfiguration.getBounds

    Spout.continually({
      val bufferedImage = robot.createScreenCapture(screenSize)
      val timestamp = System.currentTimeMillis()
      if (logging) println("grab")

      (bufferedImage, timestamp)
    }) //.asyncBoundary("blocking-io", bufferSize = 0)
  }
}
