package org.lolhens.screencapture

import java.awt._
import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import javax.imageio.ImageIO
import javax.swing.JFrame

import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.jcodec.api.awt.{AWTFrameGrab8Bit, AWTSequenceEncoder8Bit}
import org.jcodec.api.specific.AVCMP4Adaptor
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.model.Rational
import org.jcodec.containers.mp4.demuxer.MP4Demuxer
import org.lolhens.screencapture.RichObservable._
import scodec.bits.ByteVector
import swave.core.{Drain, DrainUtil, Spout, StreamEnv}

import scala.concurrent.Future

/**
  * Created by pierr on 23.11.2016.
  */
object Main {
  def main(args: Array[String]): Unit = {
    implicit val streamEnv = StreamEnv()

    val graphicsEnv = GraphicsEnvironment.getLocalGraphicsEnvironment
    val screens = graphicsEnv.getScreenDevices

    val grabber = ImageGrabber(screens(0))
    val byteStream = ImageConverter.toBytes(grabber)
    val images = ImageConverter.fromBytes(byteStream)
    val window = ImageCanvas.fullscreen(screens(0))


    images.to(window).run()
  }
}
