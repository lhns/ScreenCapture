package org.lolhens.screencapture

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.{IO, Udp}
import akka.util.ByteString
import scodec.bits.ByteVector
import swave.core.PushSpout._
import swave.core.{Drain, PushSpout, Spout}

import scala.concurrent.Future

/**
  * Created by pierr on 11.12.2016.
  */
object UdpStream {
  def receiver(bind: InetSocketAddress)(implicit actorSystem: ActorSystem): Spout[ByteVector] = {
    val pushSpout = PushSpout[ByteVector](2, 4)
    UdpReceiver.actor(bind, pushSpout)
    pushSpout.asyncBoundary("blocking-io", bufferSize = 2)
  }

  private class UdpReceiver(bind: InetSocketAddress, pushSpout: PushSpout[ByteVector]) extends Actor {

    import context.system

    IO(Udp) ! Udp.Bind(self, bind)

    override def receive: Receive = {
      case Udp.Bound(localAddress) =>
        context.become(ready(sender()))
    }

    def ready(socket: ActorRef): Receive = {
      case Udp.Received(byteString, remoteAddress) =>
        val data = ByteVector(byteString.toByteBuffer)
        pushSpout.offer(data)

      case Udp.Unbind => socket ! Udp.Unbind
      case Udp.Unbound => context.stop(self)
      case e => println(e)
    }
  }

  private object UdpReceiver {
    def props(bind: InetSocketAddress, pushSpout: PushSpout[ByteVector]) = Props(new UdpReceiver(bind, pushSpout))

    def actor(bind: InetSocketAddress, pushSpout: PushSpout[ByteVector])(implicit actorSystem: ActorSystem): ActorRef = actorSystem.actorOf(props(bind, pushSpout))
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
    }

    def ready(socket: ActorRef): Receive = {
      case Udp.Received(byteString, remoteAddress) =>

      case Udp.Unbind => socket ! Udp.Unbind
      case Udp.Unbound => context.stop(self)

      case UdpSender.SendData(data) =>
        socket ! Udp.Send(ByteString(data.toByteBuffer), remote)
    }
  }

  private object UdpSender {
    def props(bind: InetSocketAddress, remote: InetSocketAddress) = Props(new UdpSender(bind, remote))

    def actor(bind: InetSocketAddress, remote: InetSocketAddress)(implicit actorSystem: ActorSystem): ActorRef = actorSystem.actorOf(props(bind, remote))

    case class SendData(byteVector: ByteVector)

  }

}
