package org.lolhens.screencapture

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

import scodec.bits.ByteVector
import swave.core.Spout

/**
  * Created by pierr on 11.12.2016.
  */
object ImageConverter {


  def toBytes(bufferedImages: Spout[BufferedImage], format: String = "png"): Spout[ByteVector] = {
    val byteArray = new Array[Byte](262144)

    bufferedImages.map { bufferedImage =>
      val outputStream = FastByteArrayOutputStream()
      ImageIO.write(bufferedImage, format, outputStream)
      val bytes = ByteVector(outputStream.toByteArray)
      outputStream.close()
      bytes
    }
  }

  def fromBytes(byteVectors: Spout[ByteVector]): Spout[BufferedImage] =
    byteVectors.map { byteVector =>
      println(byteVector.size)
      val inputStream = new ByteArrayInputStream(byteVector.toArray)
      ImageIO.read(inputStream)
    }
}
