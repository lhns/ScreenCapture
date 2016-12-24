package org.lolhens.screencapture

import java.awt.GraphicsDevice
import java.net.InetSocketAddress

import akka.actor.ActorSystem
import cats.implicits._
import org.lolhens.screencapture.RichSpout._
import scodec.bits.ByteVector
import swave.core.StreamEnv

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by pierr on 11.12.2016.
  */
object CaptureSender {
  def apply(graphicsDevice: GraphicsDevice, remote: InetSocketAddress)(implicit streamEnv: StreamEnv, actorSystem: ActorSystem) = {
    val grabber0 = ImageGrabber(graphicsDevice)
    val grabber1 = ImageGrabber(graphicsDevice)
    val grabber2 = ImageGrabber(graphicsDevice)
    val grabber3 = ImageGrabber(graphicsDevice)
    val grabber4 = ImageGrabber(graphicsDevice)
    val grabber5 = ImageGrabber(graphicsDevice)
    val grabber6 = ImageGrabber(graphicsDevice)
    val grabber7 = ImageGrabber(graphicsDevice)
    val byteStream0 = grabber0.map(e => (ImageConverter.toBytes2(e._1, "png"), e._2)).asyncBoundary(bufferSize = 0)
    val byteStream1 = grabber1.map(e => (ImageConverter.toBytes2(e._1, "png"), e._2)).asyncBoundary(bufferSize = 0)
    val byteStream2 = grabber2.map(e => (ImageConverter.toBytes2(e._1, "png"), e._2)).asyncBoundary(bufferSize = 0)
    val byteStream3 = grabber3.map(e => (ImageConverter.toBytes2(e._1, "png"), e._2)).asyncBoundary(bufferSize = 0)
    val byteStream4 = grabber4.map(e => (ImageConverter.toBytes2(e._1, "png"), e._2)).asyncBoundary(bufferSize = 0)
    val byteStream5 = grabber5.map(e => (ImageConverter.toBytes2(e._1, "png"), e._2)).asyncBoundary(bufferSize = 0)
    val byteStream6 = grabber6.map(e => (ImageConverter.toBytes2(e._1, "png"), e._2)).asyncBoundary(bufferSize = 0)
    val byteStream7 = grabber7.map(e => (ImageConverter.toBytes2(e._1, "png"), e._2)).asyncBoundary(bufferSize = 0)

    byteStream0
      .attach(byteStream1)
      .attach(byteStream2)
      .attach(byteStream3)
      .attach(byteStream4)
      .attach(byteStream5)
      .attach(byteStream6)
      .attach(byteStream7)
      .fanInMerge(eagerComplete = true)
      .scanFlatMap[Long, Option, ByteVector](0L) { (last, e) =>
      if (e._2 >= last)
        (e._2, Some(e._1))
      else
        (last, None)
    }
      .throttle(1, 0.05d seconds)
      .via(TcpCheckedLayer.toChunks2)
      .to(TcpStream.sender(new InetSocketAddress("0.0.0.0", 0), remote)).run()
  }
}
