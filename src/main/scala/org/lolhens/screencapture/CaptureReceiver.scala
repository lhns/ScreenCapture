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
    val byteStream = TcpStream.receiver(remote).async(bufferSize = 20).scanFlatMap[(Option[Int], ByteVector), ByteVector]((None: Option[Int], ByteVector.empty)) { (last, e) =>
      val bytes = last._2 ++ e
      val (expected, leftBytes) = last._1.map(e => (e, bytes)).getOrElse {
        val result = bytes.take(4).toInt()
        (result, bytes.drop(4))
      }
      if (leftBytes.size >= expected)
        ((None, leftBytes.drop(expected)), Spout.one(leftBytes.take(expected)))
      else
        ((Some(expected), leftBytes), Spout.empty)
    }
    val images = ImageConverter.fromBytes(byteStream).map { e => println(e); e }
    val window = ImageCanvas.fullscreen(graphicsDevice)
    val frameTry = images.map(_.toOption.flatMap(Option(_))).flattenConcat().onError(_.printStackTrace()).to(window).run()
    frameTry.map(_.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE))
  }
}
