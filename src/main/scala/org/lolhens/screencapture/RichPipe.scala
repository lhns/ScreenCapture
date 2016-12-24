package org.lolhens.screencapture

import cats.MonoidK
import swave.core.{Pipe, Streamable}

import scala.language.{higherKinds, implicitConversions}

/**
  * Created by pierr on 23.12.2016.
  */
class RichPipe[A, B](val self: Pipe[A, B]) {
  def scanFlatMap[S, F[_], E](initial: S)(f: (S, B) => (S, F[E]))(implicit ev: Streamable.Aux[F[E], E], m: MonoidK[F]): self.Repr[E] =
    self.scan[(S, F[E])]((initial, m.empty[E])) { (last: (S, F[E]), e: B) =>
      f(last._1, e)
    }.flatMap(_._2)(ev)
}

object RichPipe {
  implicit def fromPipe[A, B](pipe: Pipe[A, B]): RichPipe[A, B] = new RichPipe(pipe)
}