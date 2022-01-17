/*
 * Copyright (c) 2022 Typelevel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.typelevel.jawn

import java.lang.Integer.{bitCount, highestOneBit}
import java.io.{File, FileInputStream}
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

object ChannelParser {

  final val DefaultBufferSize = 1048576

  final val ParseAsStringThreshold = 20 * 1048576

  def fromFile[J](f: File, bufferSize: Int = DefaultBufferSize): SyncParser[J] =
    if (f.length < ParseAsStringThreshold) {
      val bytes = new Array[Byte](f.length.toInt)
      val fis = new FileInputStream(f)
      fis.read(bytes)
      fis.close()
      new StringParser[J](new String(bytes, "UTF-8"))
    } else
      new ChannelParser[J](new FileInputStream(f).getChannel, bufferSize)

  def fromChannel[J](ch: ReadableByteChannel, bufferSize: Int = DefaultBufferSize): ChannelParser[J] =
    new ChannelParser[J](ch, bufferSize)

  /**
   * Given a desired buffer size, find the closest positive power-of-two larger than that size.
   *
   * This method throws an exception if the given values are negative or too large to have a valid power of two.
   */
  def computeBufferSize(x: Int): Int =
    if (x < 0)
      throw new IllegalArgumentException(s"negative bufferSize ($x)")
    else if (x > 0x40000000)
      throw new IllegalArgumentException(s"bufferSize too large ($x)")
    else if (bitCount(x) == 1)
      x
    else
      highestOneBit(x) << 1
}

/**
 * Basic file parser.
 *
 * Given a file name this parser opens it, chunks the data, and parses it.
 */
final class ChannelParser[J](ch: ReadableByteChannel, bufferSize: Int) extends SyncParser[J] with ByteBasedParser[J] {

  private[this] var Bufsize: Int = ChannelParser.computeBufferSize(bufferSize)
  private[this] var Mask: Int = Bufsize - 1
  private[this] var Allsize: Int = Bufsize * 2

  // these are the actual byte arrays we'll use
  private[this] var curr = new Array[Byte](Bufsize)
  private[this] var next = new Array[Byte](Bufsize)

  // these are the bytecounts for each array
  private[this] var ncurr = ch.read(ByteBuffer.wrap(curr))
  private[this] var nnext = ch.read(ByteBuffer.wrap(next))

  private[this] var _line = 0
  private[this] var pos = 0
  final protected[this] def newline(i: Int): Unit = { _line += 1; pos = i + 1 }
  final protected[this] def line(): Int = _line
  final protected[this] def column(i: Int): Int = i - pos

  final protected[this] def close(): Unit = ch.close()

  /**
   * Swap the curr and next arrays/buffers/counts.
   *
   * We'll call this in response to certain reset() calls. Specifically, when the index provided to reset is no longer
   * in the 'curr' buffer, we want to clear that data and swap the buffers.
   */
  final protected[this] def swap(): Unit = {
    val tmp = curr; curr = next; next = tmp
    val ntmp = ncurr; ncurr = nnext; nnext = ntmp
  }

  final protected[this] def grow(): Unit = {
    val cc = new Array[Byte](Allsize)
    System.arraycopy(curr, 0, cc, 0, Bufsize)
    System.arraycopy(next, 0, cc, Bufsize, Bufsize)

    curr = cc
    ncurr = ncurr + nnext
    next = new Array[Byte](Allsize)
    nnext = ch.read(ByteBuffer.wrap(next))

    Bufsize = Allsize
    Mask = Allsize - 1
    Allsize *= 2
  }

  /**
   * If the cursor 'i' is past the 'curr' buffer, we want to clear the current byte buffer, do a swap, load some more
   * data, and continue.
   */
  final protected[this] def reset(i: Int): Int =
    if (i >= Bufsize) {
      swap()
      nnext = ch.read(ByteBuffer.wrap(next))
      pos -= Bufsize
      i - Bufsize
    } else
      i

  final protected[this] def checkpoint(state: Int, i: Int, context: FContext[J], stack: List[FContext[J]]): Unit =
    ()

  /**
   * This is a specialized accessor for the case where our underlying data are bytes not chars.
   */
  final protected[this] def byte(i: Int): Byte =
    if (i < Bufsize) curr(i)
    else if (i < Allsize) next(i & Mask)
    else {
      grow(); byte(i)
    }

  /**
   * Reads a byte as a single Char. The byte must be valid ASCII (this method is used to parse JSON values like numbers,
   * constants, or delimiters, which are known to be within ASCII).
   */
  final protected[this] def at(i: Int): Char =
    if (i < Bufsize) curr(i).toChar
    else if (i < Allsize) next(i & Mask).toChar
    else {
      grow(); at(i)
    }

  /**
   * Access a byte range as a string.
   *
   * Since the underlying data are UTF-8 encoded, i and k must occur on unicode boundaries. Also, the resulting String
   * is not guaranteed to have length (k - i).
   */
  final protected[this] def at(i: Int, k: Int): CharSequence = {
    val len = k - i
    if (k > Allsize) {
      grow()
      at(i, k)
    } else if (k <= Bufsize)
      new String(curr, i, len, utf8)
    else if (i >= Bufsize)
      new String(next, i - Bufsize, len, utf8)
    else {
      val arr = new Array[Byte](len)
      val mid = Bufsize - i
      System.arraycopy(curr, i, arr, 0, mid)
      System.arraycopy(next, 0, arr, mid, k - Bufsize)
      new String(arr, utf8)
    }
  }

  final protected[this] def atEof(i: Int): Boolean =
    if (i < Bufsize) i >= ncurr
    else i >= (nnext + Bufsize)
}
