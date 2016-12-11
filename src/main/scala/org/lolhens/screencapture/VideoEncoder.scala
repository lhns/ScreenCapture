package org.lolhens.screencapture

/**
  * Created by pierr on 11.12.2016.
  */
object VideoEncoder {
  /*def encode(bufferedImages: Observable[BufferedImage]): Observable[ByteVector] = {
    val buffer = new BoundedEventBuffer[ByteVector]()

    val sequenceEncoder = new AWTSequenceEncoder8Bit(new SeekableByteChannelBufferWrapper(null), Rational.R(30, 1))

    /*val field = classOf[SequenceEncoder8Bit].getDeclaredField("muxer")
    field.setAccessible(true)
    field.get(sequenceEncoder).asInstanceOf[MP4Muxer].writeHeader()*/

    bufferedImages.foreach { img =>
      sequenceEncoder.encodeImage(img)
      //sequenceEncoder.finish()
    }.onSuccess {
      case _ => sequenceEncoder.finish()
    }

    Observable.fromIterator(buffer.iterator())
  }

  def decode(data: Observable[ByteVector]): Observable[BufferedImage] = {
    val c = new SeekableByteChannelObservable(data)
    println("99")
    val frameGrabber = /*frameGrab(c)*/ AWTFrameGrab8Bit.createAWTFrameGrab8Bit(c)
    println("13")
    Observable.repeatEval({
      println("a")
      val r = frameGrabber.getFrame
      println("b")
      r
    })
  }*/
}
