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

import scala.annotation.switch

/**
 * Trait used when the data to be parsed is in UTF-16.
 *
 * This parser provides parseString(). Like ByteBasedParser it has fast/slow paths for string parsing depending on
 * whether any escapes are present.
 *
 * It is simpler than ByteBasedParser.
 */
trait CharBasedParser[J] extends Parser[J] {

  final private[this] val builder = new StringBuilder()

  /**
   * See if the string has any escape sequences. If not, return the end of the string. If so, bail out and return -1.
   *
   * This method expects the data to be in UTF-16 and accesses it as chars.
   */
  final protected[this] def parseStringSimple(i: Int, ctxt: FContext[J]): Int = {
    var j = i
    var c = at(j)
    while (c != '"') {
      if (c < ' ') return die(j, s"control char (${c.toInt}) in string", 1)
      if (c == '\\') return -1
      j += 1
      c = at(j)
    }
    j + 1
  }

  /**
   * Parse a string that is known to have escape sequences.
   */
  final protected[this] def parseStringComplex(i: Int, ctxt: FContext[J]): Int = {
    var j = i + 1
    val sb = builder
    sb.setLength(0)

    var c = at(j)
    while (c != '"') {
      if (c < ' ')
        die(j, s"control char (${c.toInt}) in string", 1)
      else if (c == '\\')
        (at(j + 1): @switch) match {
          case 'b' => sb.append('\b'); j += 2
          case 'f' => sb.append('\f'); j += 2
          case 'n' => sb.append('\n'); j += 2
          case 'r' => sb.append('\r'); j += 2
          case 't' => sb.append('\t'); j += 2

          case '"' => sb.append('"'); j += 2
          case '/' => sb.append('/'); j += 2
          case '\\' => sb.append('\\'); j += 2

          // if there's a problem then descape will explode
          case 'u' =>
            val jj = j + 2
            sb.append(descape(jj, at(jj, jj + 4)))
            j += 6

          case c => die(j, s"illegal escape sequence (\\$c)", 1)
        }
      else {
        // this case is for "normal" code points that are just one Char.
        //
        // we don't have to worry about surrogate pairs, since those
        // will all be in the ranges D800–DBFF (high surrogates) or
        // DC00–DFFF (low surrogates).
        sb.append(c)
        j += 1
      }
      j = reset(j)
      c = at(j)
    }
    j += 1
    ctxt.add(sb.toString, i, j)
    j
  }

  /**
   * Parse the string according to JSON rules, and add to the given context.
   *
   * This method expects the data to be in UTF-16, and access it as Char. It performs the correct checks to make sure
   * that we don't interpret a multi-char code point incorrectly.
   */
  final protected[this] def parseString(i: Int, ctxt: FContext[J]): Int = {
    val k = parseStringSimple(i + 1, ctxt)
    if (k != -1) {
      ctxt.add(at(i + 1, k - 1), i, k)
      k
    } else
      parseStringComplex(i, ctxt)
  }
}
