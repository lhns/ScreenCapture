package swave.core

import swave.core.impl.stages.drain.ForeachDrainStage

import scala.concurrent.Promise

/**
  * Created by pierr on 11.12.2016.
  */
object DrainUtil {
  def foreachReturning[T, R](callback: T => Unit)(returnValue: R): Drain[T, R] = {
    val promise = Promise[Unit]()
    new Drain(new ForeachDrainStage(callback.asInstanceOf[AnyRef => Unit], promise), returnValue)
  }
}
