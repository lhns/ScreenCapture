package org.lolhens.screencapture

import java.awt.image.BufferedImage
import java.awt.{Rectangle, Robot, Toolkit}

import monix.reactive.Observable
import org.bytedeco.javacv._
import monix.execution.Scheduler.Implicits.global
import org.bytedeco.javacpp.helper.opencv_core.AbstractIplImage
import org.bytedeco.javacpp.{avcodec, opencv_core}
import org.bytedeco.javacpp.opencv_core.IplImage

/**
  * Created by pierr on 23.11.2016.
  */
object Main {
  def main(args: Array[String]): Unit = {
    toFrame(grabScreen(Observable.repeat(()))).foreach(println)
  }

  def grabScreen(signals: Observable[_]): Observable[BufferedImage] = {
    val robot = new Robot()
    val screenSize = new Rectangle(Toolkit.getDefaultToolkit.getScreenSize)
    signals.map(_ => robot.createScreenCapture(screenSize))
  }

  def toFrame(bufferedImages: Observable[BufferedImage]): Observable[Frame] = {
    val java2DFrameConverter = new Java2DFrameConverter()
    bufferedImages.map(bufferedImage =>  java2DFrameConverter.convert(bufferedImage))
  }

  def test = {
    val recorder = FFmpegFrameRecorder.
    recorder.setFormat(avcodec.AV_CODEC_ID_H264)

  }
}
