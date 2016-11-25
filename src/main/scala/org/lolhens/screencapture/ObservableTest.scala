package org.lolhens.screencapture

import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable

/**
  * Created by pierr on 24.11.2016.
  */
object ObservableTest {
  def scanFlatMap[T, A, B](observable: Observable[T])(initialValue: A)(f: (A, T) => (A, Observable[B])): Observable[B] =
    observable.scan((initialValue, Observable.empty[B])) {(last: (A, Observable[B]), e: T) =>
      f(last._1, e)
    }.flatMap(_._2)

  scanFlatMap[Int, Int, Int](Observable.fromIterable[Int](0 until 10))(20) {(last, e) =>
    println(s"a $last")
    (last + 1, Observable(e))
  }.foreach(e => println(s"b $e"))
}
