package org.lolhens.screencapture

import java.awt.GraphicsDevice
import java.net.InetSocketAddress

import akka.actor.ActorSystem
import swave.core.StreamEnv

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by pierr on 11.12.2016.
  */
object CaptureSender {
  def apply(graphicsDevice: GraphicsDevice, remote: InetSocketAddress)(implicit streamEnv: StreamEnv, actorSystem: ActorSystem) = {
    val grabber = ImageGrabber(graphicsDevice).async(bufferSize = 1).throttle(1, 0.3 seconds)
    val byteStream = ImageConverter.toBytes(grabber).async(bufferSize = 1)
    UdpCheckedLayer.toChunks(byteStream).to(UdpStream.sender(new InetSocketAddress("0.0.0.0", 0), remote)).run()
  }
}
