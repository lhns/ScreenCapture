package org.lolhens.screencapture

import cats.MonoidK
import swave.core.{Spout, Streamable}

import scala.language.{higherKinds, implicitConversions}

/**
  * Created by pierr on 11.12.2016.
  */
class RichSpout[A](val self: Spout[A]) {
  def scanFlatMap[S, F[_], E](initial: S)(f: (S, A) => (S, F[E]))(implicit ev: Streamable.Aux[F[E], E], m: MonoidK[F]): self.Repr[E] =
    self.scan[(S, F[E])]((initial, m.empty[E])) { (last: (S, F[E]), e: A) =>
      f(last._1, e)
    }.flatMap(_._2)(ev)
}

object RichSpout {
  implicit def fromSpout[E](spout: Spout[E]): RichSpout[E] = new RichSpout(spout)

  implicit val spoutMonoidK = new MonoidK[Spout] {
    override def empty[A]: Spout[A] = Spout.empty

    override def combineK[A](x: Spout[A], y: Spout[A]): Spout[A] = x.attach(y).fanInConcat()
  }
}