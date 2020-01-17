package org.typelevel.jawn

import java.nio.{Buffer, ByteBuffer}

/**
 * Basic ByteBuffer parser.
 *
 * This assumes that the provided ByteBuffer is ready to be read. The
 * user is responsible for any necessary flipping/resetting of the
 * ByteBuffer before parsing.
 *
 * The parser makes absolute calls to the ByteBuffer, which will not
 * update its own mutable position fields.
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
