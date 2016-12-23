package org.lolhens.screencapture

import java.awt.GraphicsDevice
import java.net.InetSocketAddress

import akka.actor.ActorSystem
import swave.core.Buffer.OverflowStrategy
import swave.core.StreamEnv

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by pierr on 11.12.2016.
  */
object CaptureSender {
  def apply(graphicsDevice: GraphicsDevice, remote: InetSocketAddress)(implicit streamEnv: StreamEnv, actorSystem: ActorSystem) = {
    val grabber = ImageGrabber(graphicsDevice).throttle(1, 0.2d seconds)
    val grabber2 = ImageGrabber(graphicsDevice).throttle(1, 0.2d seconds)
    val grabber3 = ImageGrabber(graphicsDevice).throttle(1, 0.2d seconds)
    val grabber4 = ImageGrabber(graphicsDevice).throttle(1, 0.2d seconds)
    val byteStream = ImageConverter.toBytes(grabber, "png").asyncBoundary(bufferSize = 0)
    val byteStream2 = ImageConverter.toBytes(grabber2, "png").asyncBoundary(bufferSize = 0)
    val byteStream3 = ImageConverter.toBytes(grabber3, "png").asyncBoundary(bufferSize = 0)
    val byteStream4 = ImageConverter.toBytes(grabber4, "png").asyncBoundary(bufferSize = 0)
    TcpCheckedLayer.toChunks2(
      byteStream
        .attach(byteStream2)
        .attach(byteStream3)
        .attach(byteStream4)
        .fanInMerge(eagerComplete = true).bufferDropping(1, OverflowStrategy.DropHead).throttle(1, 0.166d seconds)
    )
      .to(TcpStream.sender(new InetSocketAddress("0.0.0.0", 0), remote)).run()
    //UdpCheckedLayer.toChunks(byteStream.map(e => List(e)).flattenConcat(), 4000)
    //  .onError(_.printStackTrace())
    //  .to(UdpStream.sender(new InetSocketAddress("0.0.0.0", 0), remote)).run()
  }
}
