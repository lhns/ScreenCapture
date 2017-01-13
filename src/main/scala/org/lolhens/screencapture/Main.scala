package org.lolhens.screencapture

import java.awt.{GraphicsDevice, GraphicsEnvironment}
import java.net.InetSocketAddress

import akka.actor.ActorSystem
import fastparse.all._
import org.lolhens.screencapture.ParserUtils._
import swave.core.StreamEnv

import scala.language.postfixOps
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
  * Created by pierr on 23.11.2016.
  */
object Main {
  def main(args: Array[String]): Unit = {
    val options = Options.parse(args.mkString(" ")) match {
      case Success(options) =>
        options

      case Failure(NonFatal(exception)) =>
        exception.printStackTrace(System.out)

        println(
          """
            |Options:
            |  -h [host]     Activates the client-mode and specifies the host
            |  -p [port]     Overrides the default port (51234)
            |  -f            Activates fullscreen-mode
            |  -m [monitor]  Overrides the default monitor""".stripMargin)

        System.exit(0)
        throw new IllegalStateException()
    }

    implicit val streamEnv = StreamEnv()
    implicit val actorSystem = ActorSystem()

    options.host match {
      case Some(host) =>
        CaptureSender(selectScreen(options.monitor), new InetSocketAddress(host, options.port))

      case None =>
        CaptureReceiver(selectScreen(options.monitor), options.fullscreen, new InetSocketAddress("0.0.0.0", options.port))
    }
  }

  def selectScreen(index: Int): GraphicsDevice = {
    val graphicsEnv = GraphicsEnvironment.getLocalGraphicsEnvironment
    if (index == -1)
      graphicsEnv.getDefaultScreenDevice
    else {
      val screens = graphicsEnv.getScreenDevices
      screens(index)
    }
  }

  case class Options(host: Option[String],
                     port: Int,
                     fullscreen: Boolean,
                     monitor: Int)

  object Options {
    private val hostParser = P("-h" ~ s1 ~ text).map(Some.apply)
    private val portParser = P("-p" ~ s1 ~ number.map(_.toIntExact))
    private val fullscreenParser = P("-f".!.map(_ => true))
    private val monitorParser = P("-m" ~ s1 ~ number.map(_.toIntExact))

    private val parser =
      any(Map(
        hostParser -> hostParser,
        portParser -> portParser,
        fullscreenParser -> fullscreenParser,
        monitorParser -> monitorParser
      ))
        .rep(sep = s1)
        .map(_.toMap)
        .map { values =>
          Options(
            host = values.collectFirst { case (`hostParser`, host: Option[String@unchecked]) => host }.getOrElse(None),
            port = values.collectFirst { case (`portParser`, port: Int) => port }.getOrElse(51234),
            fullscreen = values.collectFirst { case (`fullscreenParser`, fullscreen: Boolean) => fullscreen }.getOrElse(false),
            monitor = values.collectFirst { case (`monitorParser`, monitor: Int) => monitor }.getOrElse(-1)
          )
        }

    def parse(string: String): Try[Options] = {
      (Start ~ s ~ parser ~ s ~ End)
        .parse(string)
        .tried
    }
  }

}
