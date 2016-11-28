package org.lolhens.screencapture

import java.nio.ByteBuffer

import org.jcodec.common.io.SeekableByteChannel
import scodec.bits.ByteVector

/**
  * Created by pierr on 27.11.2016.
  */
class SeekableByteChannelBufferWrapper(val buffer: BoundedEventBuffer[ByteVector]) extends SeekableByteChannel {
  private var _closed = false
  private var _position: Int = 0

  override def setPosition(l: Long): SeekableByteChannel =
    throw new UnsupportedOperationException()

  override def position(): Int = _position

  override def size(): Int = _position

  override def truncate(l: Long): SeekableByteChannel =
    throw new UnsupportedOperationException()

  override def read(dst: ByteBuffer): Int =
    throw new UnsupportedOperationException()

  override def write(src: ByteBuffer): Int = {
    val byteVector = ByteVector(src)
    val size = byteVector.size.toInt
    _position += size
    buffer += byteVector
    size
  }

  override def isOpen: Boolean = !_closed

  override def close(): Unit = _closed = true
}
