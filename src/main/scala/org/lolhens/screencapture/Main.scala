package org.lolhens.screencapture

import java.awt.GraphicsEnvironment
import java.net.InetSocketAddress

import akka.actor.ActorSystem
import swave.core.StreamEnv

import scala.language.postfixOps
import scala.util.Try

/**
  * Created by pierr on 23.11.2016.
  */
object Main {
  def main(args: Array[String]): Unit = {
    implicit val streamEnv = StreamEnv()
    implicit val actorSystem = ActorSystem()

    val screen = Try(args(0).toInt).toOption.getOrElse(-1)

    Try(args(1)).toOption match {
      case Some(receiverHost) =>
        CaptureSender(selectScreen(screen), new InetSocketAddress(receiverHost, 51234))

      case None =>
        CaptureReceiver(selectScreen(screen), new InetSocketAddress("0.0.0.0", 51234))
    }
  }

  def selectScreen(index: Int) = {
    val graphicsEnv = GraphicsEnvironment.getLocalGraphicsEnvironment
    if (index == -1)
      graphicsEnv.getDefaultScreenDevice
    else {
      val screens = graphicsEnv.getScreenDevices
      screens(index)
    }
  }
}
