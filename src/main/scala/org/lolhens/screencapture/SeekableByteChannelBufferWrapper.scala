package org.lolhens.screencapture

import java.nio.ByteBuffer

import org.jcodec.common.io.SeekableByteChannel
import scodec.bits.ByteVector

/**
  * Created by pierr on 27.11.2016.
  */
class SeekableByteChannelBufferWrapper(val buffer: BoundedEventBuffer[ByteVector]) extends SeekableByteChannel {
  private var _closed = false
  private var _size: Long = 0
  private var _position: Long = 0

  override def setPosition(position: Long): SeekableByteChannel =
    if (position >= _size) {
      _position = position
      this
    } else
      throw new UnsupportedOperationException()

  override def position(): Long = _position

  override def size(): Long = _position

  override def truncate(position: Long): SeekableByteChannel =
    if (position >= _size) {
      _position = position
      this
    } else
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
