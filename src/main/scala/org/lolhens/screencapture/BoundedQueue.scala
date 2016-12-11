package org.lolhens.screencapture

import java.util.concurrent.locks.ReentrantReadWriteLock

import monix.reactive.Observable

import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.concurrent.{Future, _}
import scala.util.Try

/**
  * Created by pierr on 02.12.2016.
  */
class BoundedQueue[E](val capacity: Option[Long] = None) {
  def this(capacity: Long) = this(Some(capacity))

  private var queue: List[E] = Nil
  private val lock = new ReentrantReadWriteLock()

  private val notFullEvent = new Object()
  private val notEmptyEvent = new Object()


  def size: Long = {
    lock.readLock().lock()
    val result = queue.size
    lock.readLock().unlock()
    result
  }

  def isEmpty: Boolean = {
    lock.readLock().lock()
    val result = queue.isEmpty
    lock.readLock().unlock()
    result
  }

  def isFull: Boolean = {
    def queueSize = size

    capacity.exists(capacity => queueSize >= capacity)
  }

  def headOption: Option[E] = {
    lock.readLock().lock()
    val result = queue.headOption
    lock.readLock().unlock()
    result
  }

  def push(elem: E): Boolean = {
    lock.writeLock().lock()
    val result = if (isFull) {
      false
    } else {
      if (isEmpty) notEmptyEvent.synchronized(notEmptyEvent.notifyAll())
      queue = queue :+ elem
      true
    }
    lock.writeLock().unlock()
    result
  }

  def pop: Option[E] = {
    lock.writeLock().lock()
    val result = headOption
    queue = queue match {
      case Nil => Nil
      case queue =>
        notFullEvent.synchronized(notFullEvent.notifyAll())
        queue.tail
    }
    lock.writeLock().unlock()
    result
  }

  def pushBlocking(elem: E)(implicit executionContext: ExecutionContext): Future[Unit] = Future {
    @tailrec
    def tryPush: Unit = if (!push(elem)) {
      blocking(Try(notFullEvent.synchronized(notFullEvent.wait(100))))
      tryPush
    }

    tryPush
  }

  def +=(elem: E)(implicit executionContext: ExecutionContext): Future[Unit] = pushBlocking(elem)

  def popBlocking(implicit executionContext: ExecutionContext): Future[E] = Future {
    @tailrec
    def tryPop: E = pop match {
      case Some(elem) =>
        elem
      case None =>
        blocking(Try(notEmptyEvent.synchronized(notEmptyEvent.wait(100))))
        tryPop
    }

    tryPop
  }

  def observable(implicit executionContext: ExecutionContext): Observable[E] = Observable.repeatEval(Await.result(popBlocking, Duration.Inf))
}
