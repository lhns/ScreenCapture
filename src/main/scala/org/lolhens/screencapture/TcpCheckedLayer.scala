package org.lolhens.screencapture

import cats.implicits._
import monix.execution.atomic.Atomic
import org.lolhens.screencapture.RichPipe._
import org.lolhens.screencapture.RichSpout._
import scodec.bits.ByteVector
import swave.core.{Pipe, Spout}

/**
  * Created by pierr on 18.12.2016.
  */
object TcpCheckedLayer {
  private val kilobyte = 1024
  private val defaultChunkSize = 64 * kilobyte

  private val packetCounter = Atomic(0L)

  def chunker(chunkSize: Int = defaultChunkSize): Pipe[ByteVector, ByteVector] =
    Pipe[ByteVector]
      .flatMap[List[ByteVector], ByteVector] { bytes: ByteVector =>
      val parts = bytes
        .grouped(chunkSize)
        .toList

      val packetNum = packetCounter.getAndIncrement()

      parts
        .zipWithIndex
        .map(e =>
          ByteVector.fromLong(packetNum + e._2) ++
            ByteVector.fromLong(packetNum) ++
            ByteVector.fromLong(packetNum + parts.size) ++
            e._1
        )
        .flatMap(e => List(e, e))
    }

  case class ChunkerState(minPacketNum: Long = 0, maxPacketNum: Long = 0, chunks: Map[Long, ByteVector] = Map.empty)

  lazy val unchunker: Pipe[ByteVector, ByteVector] =
    Pipe[ByteVector]
      .scanFlatMap[ChunkerState, List, ByteVector](ChunkerState()) { (last: ChunkerState, bytes: ByteVector) =>
      val packetNum = bytes.take(8).toLong()
      val minPacketNum = bytes.drop(8).take(8).toLong()
      val maxPacketNum = bytes.drop(16).take(8).toLong()
      val data = bytes.drop(24)

      val state =
        if (maxPacketNum != last.maxPacketNum)
          last.copy(minPacketNum = minPacketNum, maxPacketNum = maxPacketNum, chunks = Map(packetNum -> data))
        else
          last.copy(chunks = last.chunks + (packetNum -> data))

      if (state.chunks.size >= state.maxPacketNum - state.minPacketNum)
        (state.copy(chunks = Map.empty), List(ByteVector.concat(state.chunks.toList.sortBy(_._1).map(_._2))))
      else
        (state, Nil)
    }

  lazy val wrap: Pipe[ByteVector, ByteVector] = Pipe[ByteVector].map { bytes =>
    val size = ByteVector.fromInt(bytes.size.toInt)
    val sizeCheck = ByteVector.fromInt(Integer.MAX_VALUE - bytes.size.toInt)
    size ++ sizeCheck ++ bytes
  }

  private case object Err

  lazy val unwrap: Pipe[ByteVector, ByteVector] = {
    Pipe[ByteVector].scanFlatMap[ByteVector, Spout, ByteVector](ByteVector.empty) { (last, e) =>
      val buffer: ByteVector = last ++ e
      val sizeOption: Option[Either[Err.type, Int]] = Some(buffer).filter(_.size >= 8).map { buffer =>
        val size = buffer.take(4).toInt()
        if (size == Integer.MAX_VALUE - buffer.drop(4).take(4).toInt())
          Either.right(size)
        else
          Either.left(Err)
      }

      sizeOption match {
        case None =>
          (buffer, Spout.empty)

        case Some(Left(Err)) =>
          println("CORRECTING ERROR!")
          (e, Spout.empty)

        case Some(Right(size)) =>
          Some(buffer.drop(8))
            .filter(_.size >= size)
            .map(buffer => (buffer.take(size), buffer.drop(size))) match {
            case Some((data, newBuffer)) =>
              (newBuffer, Spout.one(data))

            case None =>
              (buffer, Spout.empty)
          }
      }
    }
  }
}
