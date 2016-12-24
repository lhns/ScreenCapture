package org.lolhens.screencapture

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import javax.imageio.ImageIO

import scodec.bits.ByteVector

import scala.util.Try

/**
  * Created by pierr on 11.12.2016.
  */
object ImageConverter {
  def toBytes(bufferedImage: BufferedImage, format: String = "png"): ByteVector = {
    val outputStream = new ByteArrayOutputStream()
    ImageIO.write(bufferedImage, format, outputStream)
    val bytes = ByteVector(outputStream.toByteArray)
    outputStream.close()
    bytes
  }

  def toBytes2(bufferedImage: BufferedImage, format: String = "png"): ByteVector = {
    val outputStream = FastByteArrayOutputStream()
    outputStream.synchronized {
      ImageIO.write(bufferedImage, format, outputStream)
      val bytes = ByteVector(outputStream.toByteArray)
      outputStream.close()
      bytes
    }
  }

  def fromBytes(byteVector: ByteVector): Try[BufferedImage] = {
    //println(byteVector.size)
    val inputStream = new ByteArrayInputStream(byteVector.toArray)
    Try(ImageIO.read(inputStream))
  }
}
