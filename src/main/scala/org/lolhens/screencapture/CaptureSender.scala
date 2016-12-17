package org.lolhens.screencapture

import java.awt.GraphicsDevice
import java.net.InetSocketAddress

import akka.actor.ActorSystem
import scodec.bits.ByteVector
import swave.core.StreamEnv

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by pierr on 11.12.2016.
  */
object CaptureSender {
  def apply(graphicsDevice: GraphicsDevice, remote: InetSocketAddress)(implicit streamEnv: StreamEnv, actorSystem: ActorSystem) = {
    val grabber = ImageGrabber(graphicsDevice).async(bufferSize = 1).throttle(1, 0.0166d seconds)
    //val grabber2 = ImageGrabber(graphicsDevice).async(bufferSize = 1).throttle(1, 0.0166d seconds)
    val byteStream = ImageConverter.toBytes(grabber /*.merge(grabber2)*/ , "png").async(bufferSize = 1)
    byteStream.map(b => ByteVector.fromInt(b.size.toInt) ++ b)
      .to(TcpStream.sender(new InetSocketAddress("0.0.0.0", 0), remote)).run()
    //UdpCheckedLayer.toChunks(byteStream.map(e => List(e)).flattenConcat(), 4000)
    //  .onError(_.printStackTrace())
    //  .to(UdpStream.sender(new InetSocketAddress("0.0.0.0", 0), remote)).run()
  }
}
