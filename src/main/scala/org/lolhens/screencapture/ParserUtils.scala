package org.lolhens.screencapture

import java.util.regex.Pattern

import fastparse.all._

import scala.util.{Failure, Success, Try}

/**
  * Created by u016595 on 12.09.2016.
  */
object ParserUtils {
  val space = CharPred(char => Pattern.matches("""\s""", s"$char"))
  val letter = CharIn(('A' to 'Z') ++ ('a' to 'z') ++ List('ä', 'ö', 'ü', 'ß'))
  val digit = CharIn('0' to '9')
  val separator = CharIn(List(',', ';'))
  val quote = CharIn(List('\'', '"'))
  val escape = P("\\")

  val s = NoTrace(P(space.rep))
  val s1 = P(space.rep(min = 1))

  val number: Parser[BigDecimal] =
    P("-".? ~ (digit.rep(min = 1) ~ ("." ~ digit.rep(min = 1)).? | ("." ~ digit.rep(min = 1)))).!
      .map(BigDecimal.apply)

  val quoted: Parser[String] =
    P(quote ~ ((escape ~ (quote.! | escape.!)) | (!(quote | escape) ~ AnyChar).!).rep.map(_.mkString) ~ quote)

  val text: Parser[String] = P(quoted | (!space ~ AnyChar).rep(min = 1).!)

  def any[K, V](parsers: (K, Parser[V])*): Parser[(K, V)] = P(
    parsers.toList match {
      case last :: Nil =>
        last._2.map(last._1 -> _)

      case head :: tail =>
        head._2.map(head._1 -> _) | any(tail: _*)
    }
  )

  def any[K, V](parsers: Map[K, Parser[V]]): Parser[(K, V)] = any[K, V](parsers.toList: _*)

  implicit class RichParsed[T](val parsed: Parsed[T]) extends AnyVal {
    def tried: Try[T] = parsed match {
      case Parsed.Success(result, _) =>
        Success(result)

      case failure: Parsed.Failure =>
        Failure(ParseError(failure))
    }
  }

}
