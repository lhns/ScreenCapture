package org.lolhens.screencapture

import java.awt.GraphicsDevice
import java.net.InetSocketAddress

import akka.actor.ActorSystem
import swave.core.StreamEnv

/**
  * Created by pierr on 11.12.2016.
  */
object CaptureReceiver {
  def apply(graphicsDevice: GraphicsDevice, remote: InetSocketAddress)(implicit streamEnv: StreamEnv, actorSystem: ActorSystem) = {
    val recBytes = UdpStream.receiver(remote)
    val images = ImageConverter.fromBytes(UdpCheckedLayer.fromChunks(recBytes))
    val window = ImageCanvas.fullscreen(graphicsDevice)
    images.map(_.toOption).flattenConcat().to(window).run()
  }
}
