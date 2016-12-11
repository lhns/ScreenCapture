package org.lolhens.screencapture

import java.nio.ByteBuffer

import org.jcodec.common.io.SeekableByteChannel
import scodec.bits.ByteVector
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by pierr on 27.11.2016.
  */
class SeekableByteChannelBufferWrapper(val buffer: BoundedQueue[ByteVector]) extends SeekableByteChannel {
  private var _closed = false
  private var _size: Long = 0
  private var _position: Long = 0

  private var back: Option[Long] = None
  override def setPosition(position: Long): SeekableByteChannel =
    if (position >= _size) {
      _position = position
      back.foreach {lastPos =>
        println(s"setting position from $lastPos back to $position")
        back = None
      }
      this
    } else {
      println(s"setting position from ${_position} to $position")
      back = Some(position)
      //throw new UnsupportedOperationException(s"setting position from ${_position} to $position")
      this
    }

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
    back.foreach { lastPos =>
      println(s"writing $size bytes to $lastPos")
    }
    buffer += ByteVector.low(_position - _size) ++ byteVector
    _position += size
    _size = _position
    size
  }

  override def isOpen: Boolean = !_closed

  override def close(): Unit = _closed = true
}
