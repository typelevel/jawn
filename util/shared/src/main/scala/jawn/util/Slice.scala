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

package org.typelevel.jawn.util

/**
 * Character sequence representing a lazily-calculated substring.
 *
 * This class has three constructors:
 *
 *   - Slice(s) wraps a string, ensuring that future operations (e.g. subSequence) will construct slices instead of
 *     strings.
 *
 *   - Slice(s, start, limit) is the default, and ensures that:
 *
 *   1. start >= 0 2. limit >= start 3. limit <= s.length
 *
 *   - Slice.unsafe(s, start, limit) is for situations where the above bounds-checking has already occurred. Only use
 *     this if you are absolutely sure your arguments satisfy the above invariants.
 *
 * Slice's subSequence returns another slice. This means that when wrapping a very large string, garbage collection on
 * the underlying string will not occur until all slices are freed.
 *
 * Slice's universal equality is only defined with regard to other slices. This means comparing a Slice with other
 * CharSequence values (including String) will always return false.
 *
 * Slices are serializable. However! They use the default Java serialization layout, which is not that efficient, and
 * could be a disaster in cases where a large shared string might be serialized many times in different slices.
 */
@SerialVersionUID(1L)
final class Slice private[jawn] (s: String, start: Int, limit: Int) extends CharSequence with Serializable {

  final val length: Int =
    limit - start

  def charAt(i: Int): Char =
    if (i < 0 || length <= i) throw new StringIndexOutOfBoundsException(s"index out of range: $i")
    else s.charAt(start + i)

  def subSequence(i: Int, j: Int): Slice = {
    if (i < 0) throw new StringIndexOutOfBoundsException(s"i ($i) should be >= 0")
    if (j < i) throw new StringIndexOutOfBoundsException(s"j ($j) should be >= i ($i)")
    val start2 = start + i
    val limit2 = start + j
    if (start2 > limit) throw new StringIndexOutOfBoundsException(s"i ($i) should be <= limit (${limit - start})")
    if (limit2 > limit) throw new StringIndexOutOfBoundsException(s"j ($j) should be <= limit (${limit - start})")
    Slice.unsafe(s, start2, limit2)
  }

  override def toString: String =
    s.substring(start, limit)

  override def equals(that: Any): Boolean =
    that match {
      case t: AnyRef if this eq t =>
        true
      case slice: Slice =>
        if (length != slice.length) return false
        var i: Int = 0
        while (i < length) {
          if (charAt(i) != slice.charAt(i)) return false
          i += 1
        }
        true
      case _ =>
        false
    }

  override def hashCode: Int = {
    var hash: Int = 0x90decade
    var i: Int = start
    while (i < limit) {
      hash = s.charAt(i) + (hash * 103696301) // prime
      i += 1
    }
    hash
  }
}

object Slice {

  val Empty: Slice = Slice("", 0, 0)

  def empty: Slice = Empty

  def apply(s: String): Slice =
    new Slice(s, 0, s.length)

  def apply(s: String, start: Int, limit: Int): Slice =
    if (start < 0 || limit < start || s.length < limit)
      throw new IndexOutOfBoundsException(s"invalid slice: start=$start, limit=$limit, length=${s.length}")
    else
      new Slice(s, start, limit)

  def unsafe(s: String, start: Int, limit: Int): Slice =
    new Slice(s, start, limit)
}
