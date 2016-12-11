package org.lolhens.screencapture

import java.awt.GraphicsEnvironment
import java.net.InetSocketAddress

import akka.actor.ActorSystem
import swave.core.StreamEnv

import scala.language.postfixOps

/**
  * Created by pierr on 23.11.2016.
  */
object Main {
  def main(args: Array[String]): Unit = {
    implicit val streamEnv = StreamEnv()
    implicit val actorSystem = ActorSystem()

    val graphicsEnv = GraphicsEnvironment.getLocalGraphicsEnvironment
    val screens = graphicsEnv.getScreenDevices

    CaptureSender(screens(1), new InetSocketAddress("localhost", 5123))
    CaptureReceiver(screens(0), new InetSocketAddress("localhost", 5123))
  }
}
