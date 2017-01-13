package org.lolhens.screencapture

import java.util.regex.Pattern

import fastparse.all._

import scala.util.{Failure, Success, Try}

/**
  * Created by u016595 on 12.09.2016.
  */
object ParserUtils {
  val space: Parser[Unit] =
    CharPred(char => Pattern.matches("""\s""", s"$char")).opaque("space")

  val letter: Parser[Unit] =
    CharIn(('A' to 'Z') ++ ('a' to 'z') ++ List('ä', 'ö', 'ü', 'ß')).opaque("character")

  val digit: Parser[Unit] =
    CharIn('0' to '9').opaque("digit")

  val separator: Parser[Unit] =
    CharIn(List(',', ';')).opaque("separator")

  val quote: Parser[Unit] =
    CharIn(List('\'', '"')).opaque("quote")

  val escape: Parser[Unit] =
    P("\\").opaque("escape character")

  val s: Parser[Unit] =
    NoTrace(P(space.rep)).opaque("space")

  val s1: Parser[Unit] =
    P(space.rep(min = 1)).opaque("at least 1 space")

  val number: Parser[BigDecimal] =
    P("-".? ~ (digit.rep(min = 1) ~ ("." ~ digit.rep(min = 1)).? | ("." ~ digit.rep(min = 1)))).!
      .map(BigDecimal.apply)
      .opaque("number")

  val quoted: Parser[String] =
    P(quote ~ ((escape ~ (quote.! | escape.!)) | (!(quote | escape) ~ AnyChar).!).rep.map(_.mkString) ~ quote)
      .opaque("quoted text")

  val text: Parser[String] = P(quoted | (!space ~ AnyChar).rep(min = 1).!)
    .opaque("text")

  def any[V](parsers: Parser[V]*): Parser[V] =
    parsers.toList match {
      case Nil =>
        Fail

      case last :: Nil =>
        last

      case head :: tail =>
        head | any(tail: _*)
    }

  def any[K, V](parsers: Map[K, Parser[V]]): Parser[(K, V)] =
    any[(K, V)](parsers.toList.map(e => e._2.map(e._1 -> _)): _*)

  implicit class RichParsed[T](val parsed: Parsed[T]) extends AnyVal {
    def tried: Try[T] = parsed match {
      case Parsed.Success(result, _) =>
        Success(result)

      case failure: Parsed.Failure =>
        Failure(ParseError(failure))
    }
  }

}
