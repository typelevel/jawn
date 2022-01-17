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

/**
 * FContext is used to construct nested JSON values.
 *
 * The most common cases are to build objects and arrays. However, this type is also used to build a single top-level
 * JSON element, in cases where the entire JSON document consists of "333.33".
 */
trait FContext[J] {
  def add(s: CharSequence, index: Int): Unit
  def add(s: CharSequence, start: Int, limit: Int): Unit = add(s, start)
  def add(v: J, index: Int): Unit
  def finish(index: Int): J
  def isObj: Boolean
}

object FContext {

  /**
   * A convenience trait for implementers who don't need character offsets.
   */
  trait NoIndexFContext[J] extends FContext[J] {
    def add(s: CharSequence): Unit
    def add(v: J): Unit
    def finish(): J

    final def add(s: CharSequence, index: Int): Unit = add(s)
    final def add(v: J, index: Int): Unit = add(v)
    final def finish(index: Int): J = finish()
  }
}
