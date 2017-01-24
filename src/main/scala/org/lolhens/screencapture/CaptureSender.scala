package org.lolhens.screencapture

import java.awt.GraphicsDevice
import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.stream.Materializer
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
  def apply(graphicsDevice: GraphicsDevice, remote: InetSocketAddress, parallelism: Int, fps: Double, maxLatency: Int)
           (implicit streamEnv: StreamEnv, actorSystem: ActorSystem, materializer: Materializer) = {
    val byteStreams: Seq[Spout[(ByteVector, Long)]] = for (
      _ <- 0 until parallelism;
      grabber = ImageGrabber(graphicsDevice).asyncBoundary("blocking-io", bufferSize = 0);
      byteStream = grabber.map(e => (ImageConverter.toBytes2(e._1, "png"), e._2)).asyncBoundary(bufferSize = 0)
    ) yield byteStream: Spout[(ByteVector, Long)]

    Spout.fromIterable(byteStreams)
      .flattenMerge(Math.min(parallelism, 4))
      .scanFlatMap[Long, Option, (ByteVector, Long)](0L) { (last, e) =>
      if (e._2 >= last)
        (e._2, Some(e))
      else
        (last, None)
    }
      .flatMap { e =>
        val now = System.currentTimeMillis()
        if (now - e._2 > maxLatency)
          None
        else
          Some(e._1)
      }
      .throttle(1, (1 / fps) seconds)

      .via(TcpCheckedLayer.wrap)
      .to(TcpStream.sender(new InetSocketAddress("0.0.0.0", 0), remote)).run()
  }
}
