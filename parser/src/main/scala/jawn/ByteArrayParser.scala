package org.typelevel.jawn

/**
 * Basic byte array parser.
 */
final class ByteArrayParser[J](src: Array[Byte]) extends SyncParser[J] with ByteBasedParser[J] {
  private[this] var lineState = 0
  private[this] var offset = 0
  protected[this] def line(): Int = lineState

  final protected[this] def newline(i: Int): Unit = { lineState += 1; offset = i + 1 }
  final protected[this] def column(i: Int) = i - offset

  final protected[this] def close(): Unit = ()
  final protected[this] def reset(i: Int): Int = i
  final protected[this] def checkpoint(
    state: Int,
    i: Int,
    context: FContext[J],
    stack: List[FContext[J]]
  ): Unit = {}
  final protected[this] def byte(i: Int): Byte = src(i)
  final protected[this] def at(i: Int): Char = src(i).toChar

  final protected[this] def at(i: Int, k: Int): CharSequence =
    new String(src, i, k - i, utf8)

  final protected[this] def atEof(i: Int) = i >= src.length
}
