package org.lolhens.screencapture

import java.awt.GraphicsDevice
import java.awt.event.{WindowAdapter, WindowEvent}
import java.net.InetSocketAddress
import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.stream.Materializer
import monix.execution.atomic.Atomic
import swave.core.Buffer.OverflowStrategy
import swave.core.{Spout, StreamEnv}

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by pierr on 11.12.2016.
  */
object CaptureReceiver {
  def apply(graphicsDevice: GraphicsDevice, fullscreen: Boolean, remote: InetSocketAddress, timeout: Int)
           (implicit streamEnv: StreamEnv, actorSystem: ActorSystem, materializer: Materializer) = {
    lazy val connectingImage = ImageConverter.fromBytes(ImageConverter.readFile("connecting.png"))

    val lastImage = Atomic(LocalDateTime.now())

    val byteStream = TcpStream.receiver(remote)
      .buffer(20)
      .via(TcpCheckedLayer.unwrap)
      .bufferDropping(4, OverflowStrategy.DropHead)

    val connectingImages = Spout.one(connectingImage) concat
      Spout.tick((), 1 second).flatMap { _ =>
        val now = LocalDateTime.now()
        if (now.isAfter(lastImage.get.plusSeconds(timeout)))
          Spout.one(connectingImage)
        else
          Spout.empty
      }

    val images = connectingImages merge
      byteStream.asyncBoundary(bufferSize = 0)
        .map(ImageConverter.fromBytes)

    val window = ImageCanvas.canvasDrain(graphicsDevice, fullscreen)

    val frameTry = images.map(_.toOption.flatMap(Option(_))).flattenConcat()
      .map { e => lastImage.set(LocalDateTime.now); e }
      .onError(_.printStackTrace()).to(window).run()

    frameTry.mapResult(_.map(frame => frame.addWindowListener(new WindowAdapter {
      override def windowClosing(e: WindowEvent): Unit = {
        actorSystem.terminate()
        streamEnv.shutdown()
        frame.dispose()
      }
    })))
  }
}
