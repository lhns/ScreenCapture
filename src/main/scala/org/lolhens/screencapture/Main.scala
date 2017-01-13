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
            |  -m [monitor]  Overrides the default monitor
            |  -r            Overrides the default framerate (20fps)
            |  -t            Timeout in seconds
            |  -l            Turn logging on""".stripMargin)

        System.exit(0)
        throw new IllegalStateException()
    }

    implicit val streamEnv = StreamEnv()
    implicit val actorSystem = ActorSystem()

    options.host match {
      case Some(host) =>
        CaptureSender(
          selectScreen(options.monitor),
          new InetSocketAddress(host, options.port),
          fps = options.fps
        )

      case None =>
        CaptureReceiver(
          selectScreen(options.monitor),
          options.fullscreen,
          new InetSocketAddress("0.0.0.0", options.port),
          timeout = options.timeout
        )
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
                     monitor: Int,
                     fps: Double,
                     timeout: Int,
                     logging: Boolean) {
    TcpStream.logging = logging
  }

  object Options {
    private def stringOption(name: String) = P(name ~ s1 ~ text)

    private def intParser(name: String) = P(name ~ s1 ~ number.map(_.toIntExact))

    private def doubleParser(name: String) = P(name ~ s1 ~ number.map(_.toDouble))

    private def booleanOption(name: String) = P(name.!.map(_ => true))

    private val hostParser = stringOption("-h").map(Some.apply)
    private val portParser = intParser("-p")
    private val fullscreenParser = booleanOption("-f")
    private val monitorParser = intParser("-m")
    private val framerateParser = doubleParser("-r")
    private val timeoutParser = intParser("-t")
    private val loggingParser = booleanOption("-l")

    private val parser =
      any(Map(
        hostParser -> hostParser,
        portParser -> portParser,
        fullscreenParser -> fullscreenParser,
        monitorParser -> monitorParser,
        framerateParser -> framerateParser,
        loggingParser -> loggingParser,
        timeoutParser -> timeoutParser
      ))
        .rep(sep = s1)
        .map(_.toMap)
        .map { values =>
          Options(
            host = values.collectFirst { case (`hostParser`, host: Option[String@unchecked]) => host }.getOrElse(None),
            port = values.collectFirst { case (`portParser`, port: Int) => port }.getOrElse(51234),
            fullscreen = values.collectFirst { case (`fullscreenParser`, fullscreen: Boolean) => fullscreen }.getOrElse(false),
            monitor = values.collectFirst { case (`monitorParser`, monitor: Int) => monitor }.getOrElse(-1),
            fps = values.collectFirst { case (`framerateParser`, fps: Double) => fps }.getOrElse(20),
            timeout = values.collectFirst { case (`timeoutParser`, timeout: Int) => timeout }.getOrElse(3),
            logging = values.collectFirst { case (`loggingParser`, logging: Boolean) => logging }.getOrElse(false)
          )
        }

    def parse(string: String): Try[Options] = {
      (Start ~ s ~ parser ~ s ~ End)
        .parse(string)
        .tried
    }
  }

}
