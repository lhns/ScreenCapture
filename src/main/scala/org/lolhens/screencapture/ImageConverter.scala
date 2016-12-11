package org.lolhens.screencapture

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import javax.imageio.ImageIO

import scodec.bits.ByteVector
import swave.core.Spout

/**
  * Created by pierr on 11.12.2016.
  */
object ImageConverter {
  private val byteArrayOutputStreamBuffer = {
    val field = classOf[ByteArrayOutputStream].getDeclaredField("buf")
    field.setAccessible(true)
    field
  }

  private def array2ByteArrayOutputStream(array: Array[Byte]): ByteArrayOutputStream = {
    val outputStream = new ByteArrayOutputStream(0)
    byteArrayOutputStreamBuffer.set(outputStream, array)
    outputStream
  }

  def toBytes(bufferedImages: Spout[BufferedImage], format: String = "png"): Spout[ByteVector] = {
    val byteArray = new Array[Byte](262144)

    bufferedImages.map { bufferedImage =>
      val outputStream = array2ByteArrayOutputStream(byteArray)
      ImageIO.write(bufferedImage, format, outputStream)
      ByteVector(outputStream.toByteArray)
    }
  }

  def fromBytes(byteVectors: Spout[ByteVector]): Spout[BufferedImage] =
    byteVectors.map {byteVector =>
      println(byteVector.size)
      val inputStream = new ByteArrayInputStream(byteVector.toArray)
      ImageIO.read(inputStream)
    }
}
