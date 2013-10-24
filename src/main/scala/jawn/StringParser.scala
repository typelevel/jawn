package jawn

/**
 * Basic in-memory string parsing.
 *
 * This parser is limited to the maximum string size (~2G). Obviously for large
 * JSON documents it's better to avoid using this parser and go straight from
 * disk, to avoid having to load the whole thing into memory at once.
 */
private[jawn] final class StringParser[J](s: String)
extends SyncParser[J] with CharBasedParser[J] {
  var line = 0
  final def column(i: Int) = i
  final def newline(i: Int) { line += 1 }
  final def reset(i: Int): Int = i
  final def checkpoint(state: Int, i: Int, stack: List[FContext[J]]) {}
  final def at(i: Int): Char = s.charAt(i)
  final def at(i: Int, j: Int): String = s.substring(i, j)
  final def atEof(i: Int) = i == s.length
  final def close() = ()
}
