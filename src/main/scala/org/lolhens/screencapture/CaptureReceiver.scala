package org.lolhens.screencapture

import java.awt.GraphicsDevice
import java.net.InetSocketAddress
import javax.swing.JFrame

import akka.actor.ActorSystem
import swave.core.StreamEnv

/**
  * Created by pierr on 11.12.2016.
  */
object CaptureReceiver {
  def apply(graphicsDevice: GraphicsDevice, remote: InetSocketAddress)(implicit streamEnv: StreamEnv, actorSystem: ActorSystem) = {
    val recBytes = UdpStream.receiver(remote).async(bufferSize = 1)
    val images = ImageConverter.fromBytes(UdpCheckedLayer.fromChunks(recBytes).async(bufferSize = 1))
    val window = ImageCanvas.fullscreen(graphicsDevice)
    val frameTry = images.map(_.toOption).flattenConcat().onError(_.printStackTrace()).to(window).run()
    frameTry.map(_.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE))
  }
}
