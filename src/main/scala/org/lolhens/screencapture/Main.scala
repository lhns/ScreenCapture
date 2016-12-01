package org.lolhens.screencapture

import java.awt.image.BufferedImage
import java.awt.{Rectangle, Robot, Toolkit}

import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.jcodec.api.awt.{AWTFrameGrab8Bit, AWTSequenceEncoder8Bit}
import org.jcodec.common.model.Rational
import org.lolhens.screencapture.RichObservable._
import scodec.bits.ByteVector

/**
  * Created by pierr on 23.11.2016.
  */
object Main {
  def main(args: Array[String]): Unit = {
    test1
    //Test.main
  }

  def test1 = {
    val stream = encode(grabScreen(Observable.fromIterable(0 until 2)))
    println("1")
    val g = decode(stream.map { e => println(e); e })
    println("0")
    g.foreach(e => {
      println("3")
      println(e)
    })
    println("2")

    while (true) {
      Thread.sleep(1000)
    }
  }

  def grabScreen(signals: Observable[_]): Observable[BufferedImage] = {
    val robot = new Robot()
    val screenSize = new Rectangle(Toolkit.getDefaultToolkit.getScreenSize)
    signals.map { _ =>
      println("###")
      robot.createScreenCapture(screenSize)
    }
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

  def decode(data: Observable[ByteVector]): Observable[BufferedImage] = {
    val c = new SeekableByteChannelObservable(data)
    println("99")
    val frameGrabber = AWTFrameGrab8Bit.createAWTFrameGrab8Bit(c)
    println("13")
    Observable.repeatEval({
      println("a")
      val r = frameGrabber.getFrame
      println("b")
      r
    })
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
