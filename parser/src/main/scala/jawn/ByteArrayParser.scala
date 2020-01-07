package org.typelevel.jawn

/**
 * Basic byte array parser.
 */
final class ByteArrayParser[J](src: Array[Byte]) extends SyncParser[J] with ByteBasedParser[J] {
  private[this] var lineState = 0
  private[this] var offset = 0
  protected[this] def line(): Int = lineState

  protected[this] final def newline(i: Int): Unit = { lineState += 1; offset = i + 1 }
  protected[this] final def column(i: Int) = i - offset

  protected[this] final def close(): Unit = ()
  protected[this] final def reset(i: Int): Int = i
  protected[this] final def checkpoint(state: Int, i: Int, context: RawFContext[J], stack: List[RawFContext[J]]): Unit = {}
  protected[this] final def byte(i: Int): Byte = src(i)
  protected[this] final def at(i: Int): Char = src(i).toChar

  protected[this] final def at(i: Int, k: Int): CharSequence =
    new String(src, i, k - i, utf8)

  protected[this] final def atEof(i: Int) = i >= src.length
}
