/*
 * Copyright (c) 2012 Typelevel
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

import java.nio.{Buffer, ByteBuffer}

/**
 * Basic ByteBuffer parser.
 *
 * This assumes that the provided ByteBuffer is ready to be read. The user is responsible for any necessary
 * flipping/resetting of the ByteBuffer before parsing.
 *
 * The parser makes absolute calls to the ByteBuffer, which will not update its own mutable position fields.
 */
final class ByteBufferParser[J](src: ByteBuffer) extends SyncParser[J] with ByteBasedParser[J] {
  final private[this] val start = src.position()
  final private[this] val limit = src.limit() - start

  private[this] var lineState = 0
  private[this] var offset = 0
  protected[this] def line(): Int = lineState

  final protected[this] def newline(i: Int): Unit = { lineState += 1; offset = i + 1 }
  final protected[this] def column(i: Int): Int = i - offset

  final protected[this] def close(): Unit = (src: Buffer).position(src.limit)
  final protected[this] def reset(i: Int): Int = i
  final protected[this] def checkpoint(
    state: Int,
    i: Int,
    context: FContext[J],
    stack: List[FContext[J]]
  ): Unit = {}
  final protected[this] def byte(i: Int): Byte = src.get(i + start)
  final protected[this] def at(i: Int): Char = src.get(i + start).toChar

  final protected[this] def at(i: Int, k: Int): CharSequence = {
    val len = k - i
    val arr = new Array[Byte](len)
    (src: Buffer).position(i + start)
    src.get(arr, 0, len)
    (src: Buffer).position(start)
    new String(arr, utf8)
  }

  final protected[this] def atEof(i: Int): Boolean = i >= limit
}
