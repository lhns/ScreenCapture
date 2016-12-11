package org.lolhens.screencapture

import java.awt._
import java.net.InetSocketAddress

import akka.actor.ActorSystem
import swave.core.StreamEnv

import scala.concurrent.duration._
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

    val grabber = ImageGrabber(screens(0)).async(bufferSize = 1).throttle(1, 0.4 seconds)
    val byteStream = ImageConverter.toBytes(grabber).async(bufferSize = 1)
    //byteStream.to(UdpStream.sender(new InetSocketAddress("localhost", 0), new InetSocketAddress("localhost", 5123))).run()

    val recBytes = UdpStream.receiver(new InetSocketAddress("localhost", 5123))
      .map{b =>
        println(b.size)
        b
      }
    val images = ImageConverter.fromBytes(byteStream)
    val window = ImageCanvas.fullscreen(screens(0))
    images.to(window).run()
  }
}
