package org.lolhens.screencapture

import java.awt.GraphicsDevice
import java.net.InetSocketAddress
import javax.swing.JFrame

import akka.actor.ActorSystem
import org.lolhens.screencapture.RichSpout._
import scodec.bits.ByteVector
import swave.core.{Spout, StreamEnv}

/**
  * Created by pierr on 11.12.2016.
  */
object CaptureReceiver {
  def apply(graphicsDevice: GraphicsDevice, remote: InetSocketAddress)(implicit streamEnv: StreamEnv, actorSystem: ActorSystem) = {
    //val recBytes = UdpStream.receiver(remote).async(bufferSize = 20)
    //val byteStream = UdpCheckedLayer.fromChunks(recBytes).async(bufferSize = 1).bufferDropping(2, OverflowStrategy.DropHead)
    val byteStream = TcpCheckedLayer.fromChunks2(TcpStream.receiver(remote).asyncBoundary(bufferSize = 4))
    val images = ImageConverter.fromBytes(byteStream).map { e => println(e); e }.asyncBoundary(bufferSize = 1)
    val window = ImageCanvas.fullscreen(graphicsDevice)
    val frameTry = images.map(_.toOption.flatMap(Option(_))).flattenConcat().onError(_.printStackTrace()).to(window).run()
    frameTry.mapResult(_.map(_.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)))
  }
}
