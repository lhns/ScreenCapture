package org.lolhens.screencapture

import java.awt.GraphicsDevice
import java.net.InetSocketAddress
import javax.swing.JFrame

import akka.actor.ActorSystem
import swave.core.Buffer.OverflowStrategy
import swave.core.StreamEnv

/**
  * Created by pierr on 11.12.2016.
  */
object CaptureReceiver {
  def apply(graphicsDevice: GraphicsDevice, remote: InetSocketAddress)(implicit streamEnv: StreamEnv, actorSystem: ActorSystem) = {
    val byteStream = TcpStream.receiver(remote)
      .buffer(20)
      .via(TcpCheckedLayer.fromChunks2)
      .bufferDropping(4, OverflowStrategy.DropHead)
    val images = byteStream.asyncBoundary(bufferSize = 0)
      .map(ImageConverter.fromBytes).map { e => println(e); e }
    val window = ImageCanvas.fullscreen(graphicsDevice)
    val frameTry = images.map(_.toOption.flatMap(Option(_))).flattenConcat().onError(_.printStackTrace()).to(window).run()
    frameTry.mapResult(_.map(_.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)))
  }
}
