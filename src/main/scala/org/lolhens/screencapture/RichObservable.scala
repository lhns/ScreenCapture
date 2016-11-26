package org.lolhens.screencapture

import monix.reactive.Observable

import scala.language.implicitConversions

/**
  * Created by pierr on 26.11.2016.
  */
class RichObservable[E](val self: Observable[E]) extends AnyVal {
  def scanFlatMap[A, B](initial: A)(f: (A, E) => (A, Observable[B])): Observable[B] =
    self.scan((initial, Observable.empty[B])) { (last: (A, Observable[B]), e: E) =>
      f(last._1, e)
    }.flatMap(_._2)
}

object RichObservable {
  implicit def fromObservable[E](observable: Observable[E]): RichObservable[E] = new RichObservable(observable)
}
