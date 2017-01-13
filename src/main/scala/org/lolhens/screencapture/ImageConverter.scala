package org.lolhens.screencapture

import java.awt.image.BufferedImage
import java.io.{BufferedInputStream, ByteArrayInputStream, ByteArrayOutputStream}
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

  def readFile(file: String): ByteVector = {
    val inputStream = new BufferedInputStream(getClass.getClassLoader.getResourceAsStream(file))
    val bytes = Stream.continually(inputStream.read()).takeWhile(_ != -1).map(_.toByte).toArray
    ByteVector(bytes)
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
