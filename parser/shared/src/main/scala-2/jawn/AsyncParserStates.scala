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

private[jawn] trait AsyncParserStates {

  /**
   * Explanation of the new synthetic states. The parser machinery uses positive integers for states while parsing json
   * values. We use these negative states to keep track of the async parser's status between json values.
   *
   * ASYNC_PRESTART: We haven't seen any non-whitespace yet. We could be parsing an array, or not. We are waiting for
   * valid JSON.
   *
   * ASYNC_START: We've seen an array and have begun unwrapping it. We could see a ] if the array is empty, or valid
   * JSON.
   *
   * ASYNC_END: We've parsed an array and seen the final ]. At this point we should only see whitespace or an EOF.
   *
   * ASYNC_POSTVAL: We just parsed a value from inside the array. We expect to see whitespace, a comma, or a ].
   *
   * ASYNC_PREVAL: We are in an array and we just saw a comma. We expect to see whitespace or a JSON value.
   */
  @inline final def ASYNC_PRESTART = -5
  @inline final def ASYNC_START = -4
  @inline final def ASYNC_END = -3
  @inline final def ASYNC_POSTVAL = -2
  @inline final def ASYNC_PREVAL = -1
}
