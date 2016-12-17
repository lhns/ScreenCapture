package org.lolhens.screencapture

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.ByteString
import scodec.bits.ByteVector
import swave.core.{Drain, PushSpout2, Spout}

import scala.concurrent.Future

/**
  * Created by u016595 on 13.12.2016.
  */
object TcpStream {
  def receiver(bind: InetSocketAddress)(implicit actorSystem: ActorSystem): Spout[ByteVector] = {
    val pushSpout = PushSpout2[ByteVector](2, 4)
    TcpReceiver.actor(bind, pushSpout)
    pushSpout.async("blocking-io")
  }

  private class TcpReceiver(bind: InetSocketAddress, pushSpout: PushSpout2[ByteVector]) extends Actor {

    import context.system

    IO(Tcp) ! Tcp.Bind(self, bind)

    override def receive: Receive = {
      case CommandFailed(_: Bind) =>
        context.stop(self)

      case Connected(remoteAddress, localAddress) =>
        val connection = sender()
        val handler = TcpReceiver.TcpHandler.actor(connection, pushSpout)
        connection ! Register(handler)
    }
  }

  private object TcpReceiver {
    def props(bind: InetSocketAddress, pushSpout: PushSpout2[ByteVector]) = Props(new TcpReceiver(bind, pushSpout))

    def actor(bind: InetSocketAddress, pushSpout: PushSpout2[ByteVector])(implicit actorSystem: ActorSystem): ActorRef = actorSystem.actorOf(props(bind, pushSpout))

    class TcpHandler(connection: ActorRef, pushSpout: PushSpout2[ByteVector]) extends Actor {
      override def receive: Receive = {
        case Received(byteString) =>
          val data = ByteVector(byteString.toByteBuffer)
          pushSpout.offer(data)

        case _: ConnectionClosed =>
          context.stop(self)
      }
    }

    object TcpHandler {
      def props(connection: ActorRef, pushSpout: PushSpout2[ByteVector]) = Props(new TcpHandler(connection, pushSpout))

      def actor(connection: ActorRef, pushSpout: PushSpout2[ByteVector])(implicit actorSystem: ActorSystem) = actorSystem.actorOf(props(connection, pushSpout))
    }

  }

  def sender(bind: InetSocketAddress, remote: InetSocketAddress)(implicit actorSystem: ActorSystem): Drain[ByteVector, Future[Unit]] = {
    val sender = TcpSender.actor(bind, remote)

    Drain.foreach[ByteVector] { byteVector =>
      sender ! TcpSender.SendData(byteVector)
    }.async("blocking-io")
  }

  private class TcpSender(bind: InetSocketAddress, remote: InetSocketAddress) extends Actor {

    import context.system

    IO(Tcp) ! Connect(remote)

    override def receive: Receive = {
      case CommandFailed(_: Connect) =>
        println("connect failed")
        context.stop(self)

      case Connected(remoteAddress, localAddress) =>
        val connection = sender()
        connection ! Register(self)
        context.become(ready(connection))
    }

    def ready(connection: ActorRef): Receive = {
      case TcpSender.SendData(data) =>
        println(s"sending $data")
        connection ! Write(ByteString(data.toByteBuffer))

      case f@CommandFailed(write: Write) =>
        println("write failed")
        println(f)

      case Received(data) =>

      case _: ConnectionClosed =>
        context.stop(self)
    }
  }

  private object TcpSender {
    def props(bind: InetSocketAddress, remote: InetSocketAddress) = Props(new TcpSender(bind, remote))

    def actor(bind: InetSocketAddress, remote: InetSocketAddress)(implicit actorSystem: ActorSystem): ActorRef = actorSystem.actorOf(props(bind, remote))

    case class SendData(byteVector: ByteVector)

  }

}