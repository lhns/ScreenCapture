package org.lolhens.screencapture

import java.nio.ByteBuffer

import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.jcodec.common.io.SeekableByteChannel
import scodec.bits.ByteVector

import scala.concurrent.Future

/**
  * Created by pierr on 28.11.2016.
  */
class SeekableByteChannelObservable(val observable: Observable[ByteVector]) extends SeekableByteChannel {
  private var _closed = false
  private var _size: Long = 0
  private var _position: Long = 0

  private val buffer = new BoundedEventBuffer[ByteVector](2)
  println("11")
  Future(observable.map(byteVector => buffer += byteVector).foreach(_ => ()))
  println("12")

  override def setPosition(position: Long): SeekableByteChannel = {
    println("setposition")
    if (position >= _size) {
      _position = position
      this
    } else
      throw new UnsupportedOperationException()
  }

  override def position(): Long = {
    println("position")
    _position
  }

  override def size(): Long = {
    println("size")
    _position
  }

  override def truncate(position: Long): SeekableByteChannel = {
    println("truncate")
    /*if (position >= _size) {
      _position = position
      this
    } else*/
    throw new UnsupportedOperationException()
  }

  val iterator = buffer.iterator()
  private var remainingBytes: Option[ByteBuffer] = None
println("44")
  override def read(dst: ByteBuffer): Int = {
    println("read")
    /*(0L until (_position - _size)).foreach { i =>
      if (buffer.iterator().hasNext)
        buffer.iterator().next()
    }*/
var t = 0
    while(dst.remaining() > 0) {
      t += ((if (remainingBytes.isDefined) {
        remainingBytes
      } else if (iterator.hasNext) {
        val byteVector = iterator.next()
        Some(byteVector.toByteBuffer)
      } else
        None): Option[ByteBuffer]).map { byteBuffer =>
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        val remaining = byteBuffer.remaining()
        println(dst.remaining())
        println(byteBuffer.remaining())
        println(dst.capacity())
        val limit = byteBuffer.limit()
        byteBuffer.limit(Math.min(dst.remaining(), byteBuffer.capacity()).toInt)
        dst.put(byteBuffer)
        byteBuffer.limit(limit)
        println(dst.remaining())
        val readBytes = remaining - byteBuffer.remaining()
        if (readBytes > 0)
          remainingBytes = Some(byteBuffer)
        else
          remainingBytes = None
        readBytes
      }.getOrElse(-1)
    }
    t
  }

  override def write(src: ByteBuffer): Int = {
    println("write")
    /*val byteVector = ByteVector(src)
    val size = byteVector.size.toInt
    buffer += ByteVector.low(_position - _size) ++ byteVector
    _position += size
    _size = _position
    size*/
    throw new UnsupportedOperationException()
  }

  override def isOpen: Boolean = {
    println("isopen")
    !_closed
  }

  override def close(): Unit = {
    println("close")
    _closed = true
  }
}
