package org.lolhens.screencapture

import scala.collection.mutable.{ ArrayBuffer, ObservableBuffer, Undoable }
import scala.collection.script.{ Message, Include }
import java.io.Closeable
import java.util.concurrent.LinkedBlockingQueue

/**
  * Created by pierr on 26.11.2016.
  */
class BoundedEventBuffer[T](val capacity: Int = Int.MaxValue) extends Iterable[T] with Closeable {

  protected[this] class Close[T] extends Message[T] with Undoable {
    def undo() {}
  }

  protected[this] val buffer = new ArrayBuffer[T] with ObservableBuffer[T] {
    def close(): Unit = publish(new Close[T])
  }

  /**
    * Appends the supplied item to this mutable buffer.
    *
    * @param item  the item to append
    */
  @throws[IllegalStateException]("if this buffer is closed")
  def +=(item: T): this.type = synchronized {
    if (isClosed) throw new IllegalStateException(
      "Appending to a closed buffer is not supported"
    )
    while (buffer.size > capacity) Thread.sleep(1000) // TODO
    buffer += item
    //0 until (buffer.size - capacity) foreach buffer.remove
    this
  }

  /**
    * Appends the supplied items to this mutable buffer.
    *
    * @param items  the items to append
    */
  @throws[IllegalStateException]("if this buffer is closed")
  def ++=(items: Traversable[T]): this.type = {
    items foreach +=
    this
  }

  private[this] var _isClosed = false

  /**
    * Closes this buffer, after which appending subsequent items is not
    * supported.
    */
  def close(): Unit = synchronized {
    _isClosed = true
    buffer.close
  }

  /**
    * Returns `true` if this buffer is closed.
    */
  def isClosed(): Boolean = _isClosed

  /**
    * Returns a snapshot of the items currently contained in this buffer.
    */
  def bufferedItems(): Iterable[T] = buffer.toIndexedSeq

  /**
    * Returns an iterator over subsequently appended items (not including
    * items already contained in this buffer).  The returned iterator's
    * `next` method blocks, and therefore does not have a definite size and
    * does not terminate until this buffer has been closed.
    */
  def futureItems(): Iterator[T] = synchronized {
    new buffer.Sub with Iterator[Option[T]] {
      val internalQueue = new LinkedBlockingQueue[Option[T]]
      buffer.subscribe(this)

      def notify(pub: buffer.Pub, message: Message[T] with Undoable): Unit =
        message match {
          case include: Include[T] => internalQueue add Some(include.elem)
          case close: Close[T]     => internalQueue add None
          case _                   => ()
        }

      def hasNext(): Boolean = !isClosed

      def next(): Option[T] = internalQueue.take
    }.takeWhile(_.isDefined).flatten
  }

  /**
    * Returns an iterator containing the items contained in this buffer,
    * followed by any subsequently appended items.  The returned iterator's
    * `next` method blocks, and therefore does not have a definite size and
    * does not terminate until this buffer has been closed.
    */
  def iterator(): Iterator[T] = synchronized {
    bufferedItems.iterator ++ futureItems
  }

  override def toString(): String =
    "BoundedEventBuffer(%s)" format buffer.mkString(", ")

}
