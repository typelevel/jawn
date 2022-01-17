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

package object util {

  /**
   * Parse the given character sequence as a single Long value (64-bit signed integer) in decimal (base-10).
   *
   * Other than "0", leading zeros are not allowed, nor are leading plusses. At most one leading minus is allowed. The
   * value "-0" is allowed, and is interpreted as 0.
   *
   * Stated more precisely, accepted values:
   *
   *   - conform to the pattern: -?(0|([1-9][0-9]*))
   *   - are within [-9223372036854775808, 9223372036854775807]
   *
   * This method will throw an `InvalidLong` exception on invalid input.
   */
  def parseLong(cs: CharSequence): Long = {

    // we store the inverse of the positive sum, to ensure we don't
    // incorrectly overflow on Long.MinValue. for positive numbers
    // this inverse sum will be inverted before being returned.
    var inverseSum: Long = 0L
    var inverseSign: Long = -1L
    var i: Int = 0

    if (cs.charAt(0) == '-') {
      inverseSign = 1L
      i = 1
    }

    val len = cs.length
    val size = len - i
    if (i >= len) throw InvalidLong(cs.toString)
    if (size > 19) throw InvalidLong(cs.toString)
    if (cs.charAt(i) == '0' && size > 1) throw InvalidLong(cs.toString)

    while (i < len) {
      val digit = cs.charAt(i).toInt - 48
      if (digit < 0 || 9 < digit) throw InvalidLong(cs.toString)
      inverseSum = inverseSum * 10L - digit
      i += 1
    }

    // detect and throw on overflow
    if (size == 19 && (inverseSum >= 0 || (inverseSum == Long.MinValue && inverseSign < 0)))
      throw InvalidLong(cs.toString)

    inverseSum * inverseSign
  }

  /**
   * Parse the given character sequence as a single Long value (64-bit signed integer) in decimal (base-10).
   *
   * For valid inputs, this method produces the same values as `parseLong`. However, by avoiding input validation it is
   * up to 50% faster.
   *
   * For inputs which `parseLong` throws an error on, `parseLongUnsafe` may (or may not) throw an error, or return a
   * bogus value. This method makes no guarantees about how it handles invalid input.
   *
   * This method should only be used on sequences which have already been parsed (e.g. by a Jawn parser). When in doubt,
   * use `parseLong(cs)`, which is still significantly faster than `java.lang.Long.parseLong(cs.toString)`.
   */
  def parseLongUnsafe(cs: CharSequence): Long = {

    // we store the inverse of the positive sum, to ensure we don't
    // incorrectly overflow on Long.MinValue. for positive numbers
    // this inverse sum will be inverted before being returned.
    var inverseSum: Long = 0L
    var inverseSign: Long = -1L
    var i: Int = 0

    if (cs.charAt(0) == '-') {
      inverseSign = 1L
      i = 1
    }

    val len = cs.length
    while (i < len) {
      inverseSum = inverseSum * 10L - (cs.charAt(i).toInt - 48)
      i += 1
    }

    inverseSum * inverseSign
  }
}
