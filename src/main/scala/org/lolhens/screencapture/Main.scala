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
    toFrame(grabScreen(Observable.repeat(())))

    while (true) {
      Thread.sleep(1000)
    }
    //toFrame(grabScreen(Observable.repeat(()))).foreach(println)
  }

  def grabScreen(signals: Observable[_]): Observable[BufferedImage] = {
    val robot = new Robot()
    val screenSize = new Rectangle(Toolkit.getDefaultToolkit.getScreenSize)
    signals.map(_ => robot.createScreenCapture(screenSize))
  }

  def toFrame(bufferedImages: Observable[BufferedImage]): Unit = {
    val ch = new SeekableByteChannel {
      private var closed = false
      private var pos: Int = 0
      private val buffer = new BoundedEventBuffer[ByteVector]()

      override def setPosition(l: Long): SeekableByteChannel =
        throw new UnsupportedOperationException()

      override def position(): Int = pos

      override def size(): Int = pos

      override def truncate(l: Long): SeekableByteChannel =
        throw new UnsupportedOperationException()

      override def read(dst: ByteBuffer): Int =
        throw new UnsupportedOperationException()

      override def write(src: ByteBuffer): Int = {
        val byteVector = ByteVector(src)
        val size = byteVector.size.toInt
        pos += size
        buffer += byteVector
        size
      }

      override def isOpen: Boolean = !closed

      override def close(): Unit = closed = true
    }

    val sequenceEncoder = new AWTSequenceEncoder8Bit(ch, Rational.R(1, 1))

    bufferedImages.foreach(img => sequenceEncoder.encodeImage(img)).onSuccess {
      case _ => sequenceEncoder.finish()
    }
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
