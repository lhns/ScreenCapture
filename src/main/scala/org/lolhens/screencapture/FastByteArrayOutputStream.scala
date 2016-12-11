package org.lolhens.screencapture

import java.io.ByteArrayOutputStream

/**
  * Created by pierr on 11.12.2016.
  */
object FastByteArrayOutputStream {
  private val arrays = new ThreadLocal[Array[Byte]] {
    override def initialValue(): Array[Byte] = new Array[Byte](0)
  }

  private val byteArrayOutputStreamBufField = {
    val field = classOf[ByteArrayOutputStream].getDeclaredField("buf")
    field.setAccessible(true)
    field
  }

  def apply(): ByteArrayOutputStream = {
    val outputStream = new ByteArrayOutputStream(0) {
      override def close(): Unit = {
        super.close()

        val buf = byteArrayOutputStreamBufField.get(this: ByteArrayOutputStream).asInstanceOf[Array[Byte]]
        if (buf.length > arrays.get().length) {
          arrays.set(buf)
        }
      }
    }

    byteArrayOutputStreamBufField.set(outputStream, arrays.get())

    outputStream
  }
}
