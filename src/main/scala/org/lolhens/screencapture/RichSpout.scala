package org.lolhens.screencapture

import swave.core.Spout

/**
  * Created by pierr on 11.12.2016.
  */
class RichSpout[E](val self: Spout[E]) {
  def scanFlatMap[A, B](initial: A)(f: (A, E) => (A, Spout[B])): Spout[B] =
    self.scan((initial, Spout.empty[B])) { (last: (A, Spout[B]), e: E) =>
      f(last._1, e)
    }.map(_._2).flattenConcat()
}

object RichSpout {
  implicit def fromSpout[E](spout: Spout[E]): RichSpout[E] = new RichSpout(spout)
}