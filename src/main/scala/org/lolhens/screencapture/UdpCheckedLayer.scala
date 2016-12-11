package org.lolhens.screencapture

import org.lolhens.screencapture.RichSpout._
import scodec.bits.ByteVector
import swave.core.Spout

/**
  * Created by pierr on 11.12.2016.
  */
object UdpCheckedLayer {
  //protocol: chunkNum: Int ~ maxChunkNum: Int ~ chunkSize: Int ~ data: Bytes
  def toChunks(byteVectors: Spout[ByteVector], chunkSize: Int = 10000): Spout[ByteVector] =
    byteVectors.map { byteVector =>
      val dataPackets = byteVector.grouped(chunkSize).toList
      if (dataPackets.nonEmpty) {
        val packetsWithIndex = dataPackets.zipWithIndex
        val maxChunkNum = packetsWithIndex.size - 1
        val packets = packetsWithIndex.map {
          case (packet, i) =>
            ByteVector.fromInt(i) ++ ByteVector.fromInt(maxChunkNum) ++ ByteVector.fromInt(packet.intSize.get) ++ packet
        }
        packets
      } else
        Nil
    }
      .flattenConcat()

  def fromChunks(byteVectors: Spout[ByteVector]): Spout[ByteVector] =
    byteVectors.scanFlatMap[(Int, Map[Int, ByteVector]), ByteVector]((0, Map.empty)) { (last, e) =>
      val chunkNum = e.take(4).toInt()
      val maxChunkNum = e.drop(4).take(4).toInt()
      val packetSize = e.drop(8).take(4).toInt()
      val packet = e.drop(12)
      if (packet.intSize.get == packetSize) {
        val collectedPackets = last._2 + (chunkNum -> packet)
        val sortedPackets = (0 to maxChunkNum).map(collectedPackets.get)
        val missing = sortedPackets.exists(_.isEmpty)
        if (missing) {
          ((maxChunkNum, collectedPackets), Spout.empty)
        } else {
          val byteVector = ByteVector.concat(sortedPackets.map(_.get))
          ((0, Map.empty), Spout.one(byteVector))
        }
      } else {
        (last, Spout.empty)
      }
    }
}
