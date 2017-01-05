package org.lolhens.screencapture

import java.awt.GraphicsDevice
import java.net.InetSocketAddress

import akka.actor.ActorSystem
import cats.implicits._
import org.lolhens.screencapture.RichSpout._
import scodec.bits.ByteVector
import swave.core.{Spout, StreamEnv}

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by pierr on 11.12.2016.
  */
object CaptureSender {
  def apply(graphicsDevice: GraphicsDevice, remote: InetSocketAddress)(implicit streamEnv: StreamEnv, actorSystem: ActorSystem) = {
    val byteStreams: Seq[Spout[(ByteVector, Long)]] = for (
      _ <- 0 until Runtime.getRuntime.availableProcessors();
      grabber = ImageGrabber(graphicsDevice);
      byteStream = grabber.map(e => (ImageConverter.toBytes2(e._1, "png"), e._2)).asyncBoundary(bufferSize = 0)
    ) yield byteStream: Spout[(ByteVector, Long)]

    Spout.fromIterable(byteStreams)
      .flattenMerge(2)
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
