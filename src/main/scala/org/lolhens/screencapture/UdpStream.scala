package org.lolhens.screencapture

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.{IO, Udp, UdpConnected}
import akka.util.ByteString
import scodec.bits.ByteVector
import swave.core.PushSpout2._
import swave.core.{Drain, PushSpout2, Spout}

import scala.concurrent.Future

/**
  * Created by pierr on 11.12.2016.
  */
object UdpStream {
  def receiver(remote: InetSocketAddress)(implicit actorSystem: ActorSystem): Spout[ByteVector] = {
    val pushSpout = PushSpout2[ByteVector](2, 4)
    UdpReceiver.actor(remote, pushSpout)
    pushSpout.async("blocking-io")
  }

  private class UdpReceiver(remote: InetSocketAddress, pushSpout: PushSpout2[ByteVector]) extends Actor {

    import context.system

    IO(UdpConnected) ! UdpConnected.Connect(self, remote)

    override def receive: Receive = {
      case UdpConnected.Connected =>
        context.become(ready(sender()))
        println("testR")
    }

    def ready(connection: ActorRef): Receive = {
      case UdpConnected.Received(byteString) =>
        val data = ByteVector(byteString.toByteBuffer)
        println("rec")
        pushSpout.offer(data)

      case UdpConnected.Disconnect => connection ! UdpConnected.Disconnect
      case UdpConnected.Disconnected => context.stop(self)
    }
  }

  private object UdpReceiver {
    def props(remote: InetSocketAddress, pushSpout: PushSpout2[ByteVector]) = Props(new UdpReceiver(remote, pushSpout))

    def actor(remote: InetSocketAddress, pushSpout: PushSpout2[ByteVector])(implicit actorSystem: ActorSystem): ActorRef = actorSystem.actorOf(props(remote, pushSpout))
  }

  def sender(bind: InetSocketAddress, remote: InetSocketAddress)(implicit actorSystem: ActorSystem): Drain[ByteVector, Future[Unit]] = {
    val sender = UdpSender.actor(bind, remote)

    Drain.foreach[ByteVector] { byteVector =>
      sender ! UdpSender.SendData(byteVector)
    }.async("blocking-io")
  }

  private class UdpSender(bind: InetSocketAddress, remote: InetSocketAddress) extends Actor {

    import context.system

    IO(Udp) ! Udp.Bind(self, bind)

    override def receive: Receive = {
      case Udp.Bound(localAddress) =>
        context.become(ready(sender()))
        println("testS")
    }

    def ready(socket: ActorRef): Receive = {
      case Udp.Received(byteString, remoteAddress) =>

      case Udp.Unbind => socket ! Udp.Unbind
      case Udp.Unbound => context.stop(self)

      case UdpSender.SendData(byteVector) =>
        val data = ByteString(byteVector.toByteBuffer)
        println(s"send ${data.size}")
        socket ! Udp.Send(data.take(100), remote)
    }
  }

  private object UdpSender {
    def props(bind: InetSocketAddress, remote: InetSocketAddress) = Props(new UdpSender(bind, remote))

    def actor(bind: InetSocketAddress, remote: InetSocketAddress)(implicit actorSystem: ActorSystem): ActorRef = actorSystem.actorOf(props(bind, remote))

    case class SendData(byteVector: ByteVector)

  }

}
