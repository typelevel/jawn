package jawn

/**
 * Character sequence representing a lazily-calculated substring.
 *
 * This class has three constructors:
 *
 *  - Slice(s) wraps a string, ensuring that future operations
 *    (e.g. subSequence) will construct slices instead of strings.
 *
 *  - Slice(s, start, limit) is the default, and ensures that:
 *
 *     1. start >= 0
 *     2. limit >= start
 *     3. limit <= s.length
 *
 *  - Slice.unsafe(s, start, limit) is for situations where the above
 *    bounds-checking has already occurred. Only use this if you are
 *    absolutely sure your arguments satisfy the above invariants.
 *
 * Slice's subSequence returns another slice. This means that when
 * wrapping a very large string, garbage collection on the underlying
 * string will not occur until all slices are freed.
 */
final class Slice private[jawn] (s: String, start: Int, limit: Int) extends CharSequence {
  def length: Int =
    limit - start
  def charAt(i: Int): Char =
    if (i < 0 || length <= i) throw new StringIndexOutOfBoundsException(s"index out of range: $i")
    else s.charAt(start + i)
  def subSequence(i: Int, j: Int): Slice =
    Slice(s, start + i, start + j)
  override def toString: String =
    s.substring(start, limit)
}

object Slice {

  val Empty: Slice = Slice("", 0, 0)

  def empty: Slice = Empty

  def apply(s: String): Slice =
    new Slice(s, 0, s.length)

  def apply(s: String, start: Int, limit: Int): Slice =
    if (start < 0 || limit < start || s.length < limit) {
      throw new IndexOutOfBoundsException(s"invalid slice: start=$start, limit=$limit, length=${s.length}")
    } else {
      new Slice(s, start, limit)
    }

  def unsafe(s: String, start: Int, limit: Int): Slice =
    new Slice(s, start, limit)
}
