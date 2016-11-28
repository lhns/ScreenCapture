package org.lolhens.screencapture

import java.awt.image.BufferedImage
import java.awt.{Rectangle, Robot, Toolkit}
import java.nio.ByteBuffer

import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.jcodec.api.awt.AWTSequenceEncoder8Bit
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.model.Rational
import org.lolhens.screencapture.RichObservable._
import scodec.bits.ByteVector

/**
  * Created by pierr on 23.11.2016.
  */
object Main {
  def main(args: Array[String]): Unit = {
    encode(grabScreen(Observable.repeat(()))).foreach(println)

    while (true) {
      Thread.sleep(1000)
    }
  }

  def grabScreen(signals: Observable[_]): Observable[BufferedImage] = {
    val robot = new Robot()
    val screenSize = new Rectangle(Toolkit.getDefaultToolkit.getScreenSize)
    signals.map(_ => robot.createScreenCapture(screenSize))
  }

  def encode(bufferedImages: Observable[BufferedImage]): Observable[ByteVector] = {
    val buffer = new BoundedEventBuffer[ByteVector]()

    val sequenceEncoder = new AWTSequenceEncoder8Bit(new SeekableByteChannelBufferWrapper(buffer), Rational.R(30, 1))

    bufferedImages.foreach { img =>
      sequenceEncoder.encodeImage(img)
    }.onSuccess {
      case _ => sequenceEncoder.finish()
    }

    Observable.fromIterator(buffer.iterator())
  }

  def test = {
    //val recorder = FFmpegFrameRecorder.
    //recorder.setFormat(avcodec.AV_CODEC_ID_H264)


    Observable.fromIterable[Int](0 until 10).scanFlatMap[Int, Int](20) { (last, e) =>
      println(s"a $last")
      (last + 1, Observable(e))
    }.foreach(e => println(s"b $e"))
  }
}
