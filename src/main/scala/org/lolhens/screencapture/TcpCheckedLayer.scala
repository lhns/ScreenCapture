package org.lolhens.screencapture

import cats.implicits._
import org.lolhens.screencapture.RichPipe._
import org.lolhens.screencapture.RichSpout._
import scodec.bits.ByteVector
import swave.core.{Pipe, Spout}

import scala.Option.option2Iterable

/**
  * Created by pierr on 18.12.2016.
  */
object TcpCheckedLayer {
  def toChunks(byteVectors: Spout[ByteVector], chunkSize: Int = 512): Pipe[ByteVector, ByteVector] = {
    Pipe[ByteVector].map[ByteVector] { bytes: ByteVector =>
      val size = ByteVector.fromInt(bytes.size.toInt)
      //val size2 = ByteVector.fromInt(bytes.size.toInt + 2)
      println(bytes.size)
      ByteVector.concat(bytes.grouped(chunkSize).map(e => size ++ /*ByteVector.fromInt(e.size.toInt) ++*/ e))
    }
  }

  case class State(inputBuffer: ByteVector, dataSize: Option[Int], dataBlocks: ByteVector)

  object State {
    val empty = State(ByteVector.empty, None, ByteVector.empty)
  }

  def fromChunks(byteVectors: Spout[ByteVector], chunkSize: Int = 512): Spout[ByteVector] = {
    byteVectors.scanFlatMap[State, Spout, ByteVector](State.empty) { (last, e) =>
      val newInputBuffer = last.inputBuffer ++ e
      println(newInputBuffer)

      val newDataSize = Some(newInputBuffer).filter(_.size >= 4).map(_.take(4).toInt())
      println(newDataSize)

      if (last.dataSize.zip(newDataSize).exists(e => e._1 != e._2))
        (State(e, None, ByteVector.empty), Spout.empty)
      else {
        val newNewDataSize = last.dataSize.getOrElse(newDataSize.get)
        val currentChunkSize: Int = last.dataSize.map(_ - last.dataBlocks.size.toInt).filter(_ <= chunkSize).getOrElse(chunkSize)
        val (newDataBlocks, newNewInputBuffer) =
          if (newInputBuffer.size >= 4 + currentChunkSize)
            (last.dataBlocks ++ newInputBuffer.drop(4).take(currentChunkSize), newInputBuffer.drop(4).drop(currentChunkSize))
          else
            (last.dataBlocks, newInputBuffer)

        if (newDataBlocks.size >= newNewDataSize) {
          println("emit")
          (State(newNewInputBuffer, Some(newNewDataSize), newDataBlocks.drop(newNewDataSize)), Spout.one(newDataBlocks.take(newNewDataSize)))
        } else
          (State(newNewInputBuffer, Some(newNewDataSize), newDataBlocks), Spout.empty)
      }
    }
      .map { e: ByteVector => println(s"received $e"); e }
  }

  val toChunks2: Pipe[ByteVector, ByteVector] = Pipe[ByteVector].map { bytes =>
    val size = ByteVector.fromInt(bytes.size.toInt)
    val sizeCheck = ByteVector.fromInt(Integer.MAX_VALUE - bytes.size.toInt)
    size ++ sizeCheck ++ bytes
  }

  private case object Err

  val fromChunks2: Pipe[ByteVector, ByteVector] = {
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
