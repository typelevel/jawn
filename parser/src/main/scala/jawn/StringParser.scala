package jawn

/**
 * Basic in-memory string parsing.
 *
 * This is probably the simplest Parser implementation, since there is
 * no UTF-8 decoding, and the data is already fully available.
 *
 * This parser is limited to the maximum string size (~2G). Obviously
 * for large JSON documents it's better to avoid using this parser and
 * go straight from disk, to avoid having to load the whole thing into
 * memory at once. So this limit will probably not be a problem in
 * practice.
 */
private[jawn] final class StringParser[J](s: String) extends SyncParser[J] with CharBasedParser[J] {
  var line = 0
  final def column(i: Int) = i
  final def newline(i: Int) { line += 1 }
  final def reset(i: Int): Int = i
  final def checkpoint(state: Int, i: Int, stack: List[FContext[J]]) {}
  final def at(i: Int): Char = s.charAt(i)
  final def at(i: Int, j: Int): CharSequence = new Slice(s, i, j)
  final def atEof(i: Int) = i == s.length
  final def close() = ()
}

import java.lang.Math

final class Slice(s: String, start: Int, limit: Int) extends CharSequence {
  def charAt(k: Int): Char =
    s.charAt(start + k)
  def length: Int =
    limit - start
  def subSequence(i: Int, j: Int): CharSequence =
    new Slice(s, start + i, Math.min(start + j, limit))
  override def equals(that: Any): Boolean =
    that match {
      case cs: CharSequence =>
        if (this eq cs) return true
        val len = length
        if (len != cs.length) return false
        var i = 0
        while (i < len) {
          if (charAt(i) != cs.charAt(i)) return false
          i += 1
        }
        true
      case _ =>
        false
    }

  // uses a starting prime number and a prime multiplier to get good
  // hashing behavior in most cases.
  override def hashCode: Int = {
    var code: Int = 3267000013 // prime
    var i: Int = 0
    while (i < len) {
      code = code * 3628273133 + charAt(i)
      i += 1
    }
    code
  }

  override def toString: String =
    s.substring(start, limit)
}
