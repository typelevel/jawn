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

import scala.annotation.switch
import scala.math.max
import scala.collection.mutable
import scala.util.control
import java.nio.ByteBuffer

object AsyncParser {

  sealed abstract class Mode(val start: Int, val value: Int)
  case object UnwrapArray extends Mode(-5, 1)
  case object ValueStream extends Mode(-1, 0)
  case object SingleValue extends Mode(-1, -1)

  def apply[J](mode: Mode = SingleValue): AsyncParser[J] =
    new AsyncParser(
      state = mode.start,
      curr = 0,
      context = null,
      stack = Nil,
      data = new Array[Byte](131072),
      len = 0,
      allocated = 131072,
      offset = 0,
      done = false,
      streamMode = mode.value,
      multiValue = false
    )

  def apply[J](mode: Mode, multiValue: Boolean): AsyncParser[J] =
    new AsyncParser(
      state = mode.start,
      curr = 0,
      context = null,
      stack = Nil,
      data = new Array[Byte](131072),
      len = 0,
      allocated = 131072,
      offset = 0,
      done = false,
      streamMode = mode.value,
      multiValue = multiValue
    )
}

/**
 * AsyncParser is able to parse chunks of data (encoded as Option[ByteBuffer] instances) and parse asynchronously. You
 * can use the factory methods in the companion object to instantiate an async parser.
 *
 * The async parser's fields are described below:
 *
 * The (state, curr, stack) triple is used to save and restore parser state between async calls. State also helps encode
 * extra information when streaming or unwrapping an array.
 *
 * The (data, len, allocated) triple is used to manage the underlying data the parser is keeping track of. As new data
 * comes in, data may be expanded if not enough space is available.
 *
 * The offset parameter is used to drive the outer async parsing. It stores similar information to curr but is kept
 * separate to avoid "corrupting" our snapshot.
 *
 * The done parameter is used internally to help figure out when the atEof() parser method should return true. This will
 * be set when apply(None) is called.
 *
 * The streamMode parameter controls how the asynchronous parser will be handling multiple values. There are three
 * states:
 *
 * 1: An array is being unwrapped. Normal JSON array rules apply (Note that if the outer value observed is not an array,
 * this mode will toggle to the -1 mode).
 *
 * 0: A stream of individual JSON elements separated by whitespace are being parsed. We can return each complete element
 * as we parse it.
 *
 * -1: No streaming is occuring. Only a single JSON value is allowed.
 */
final class AsyncParser[J] protected[jawn] (
  protected[jawn] var state: Int,
  protected[jawn] var curr: Int,
  protected[jawn] var context: FContext[J],
  protected[jawn] var stack: List[FContext[J]],
  protected[jawn] var data: Array[Byte],
  protected[jawn] var len: Int,
  protected[jawn] var allocated: Int,
  protected[jawn] var offset: Int,
  protected[jawn] var done: Boolean,
  protected[jawn] var streamMode: Int,
  protected[jawn] val multiValue: Boolean
) extends ByteBasedParser[J]
    with AsyncParserStates {

  protected[jawn] def this(state: Int,
                           curr: Int,
                           context: FContext[J],
                           stack: List[FContext[J]],
                           data: Array[Byte],
                           len: Int,
                           allocated: Int,
                           offset: Int,
                           done: Boolean,
                           streamMode: Int
  ) =
    this(state, curr, context, stack, data, len, allocated, offset, done, streamMode, multiValue = false)

  private[this] var _line = 0
  protected[this] var pos = 0
  final protected[this] def newline(i: Int): Unit = { _line += 1; pos = i + 1 }
  final protected[this] def line(): Int = _line
  final protected[this] def column(i: Int): Int = i - pos

  final def copy(): AsyncParser[J] =
    new AsyncParser(state, curr, context, stack, data.clone, len, allocated, offset, done, streamMode, multiValue)

  final def absorb(buf: ByteBuffer)(implicit facade: Facade[J]): Either[ParseException, collection.Seq[J]] = {
    done = false
    val buflen = buf.limit() - buf.position()
    val need = len + buflen
    resizeIfNecessary(need)
    buf.get(data, len, buflen)
    len = need
    churn()
  }

  final def absorb(bytes: Array[Byte])(implicit facade: Facade[J]): Either[ParseException, collection.Seq[J]] =
    absorb(ByteBuffer.wrap(bytes))

  final def absorb(s: String)(implicit facade: Facade[J]): Either[ParseException, collection.Seq[J]] =
    absorb(ByteBuffer.wrap(s.getBytes(utf8)))

  final def finalAbsorb(buf: ByteBuffer)(implicit facade: Facade[J]): Either[ParseException, collection.Seq[J]] =
    absorb(buf)(facade) match {
      case Right(xs) =>
        finish()(facade) match {
          case Right(ys) => Right(xs ++ ys)
          case left1 @ Left(_) => left1
        }
      case left0 @ Left(_) => left0
    }

  final def finalAbsorb(bytes: Array[Byte])(implicit facade: Facade[J]): Either[ParseException, collection.Seq[J]] =
    finalAbsorb(ByteBuffer.wrap(bytes))

  final def finalAbsorb(s: String)(implicit facade: Facade[J]): Either[ParseException, collection.Seq[J]] =
    finalAbsorb(ByteBuffer.wrap(s.getBytes(utf8)))

  final def finish()(implicit facade: Facade[J]): Either[ParseException, collection.Seq[J]] = {
    done = true
    churn()
  }

  final protected[this] def resizeIfNecessary(need: Int): Unit =
    // if we don't have enough free space available we'll need to grow our
    // data array. we never shrink the data array, assuming users will call
    // feed with similarly-sized buffers.
    if (need > allocated) {
      val doubled = if (allocated < 0x40000000) allocated * 2 else Int.MaxValue
      val newsize = max(need, doubled)
      val newdata = new Array[Byte](newsize)
      System.arraycopy(data, 0, newdata, 0, len)
      data = newdata
      allocated = newsize
    }

  protected[jawn] def churn()(implicit facade: Facade[J]): Either[ParseException, collection.Seq[J]] = {

    // accumulates json values
    val results = mutable.ArrayBuffer.empty[J]

    // we rely on exceptions to tell us when we run out of data
    try {
      while (true)
        if (state < 0)
          (at(offset): @switch) match {
            case '\n' =>
              newline(offset)
              offset += 1

            case ' ' | '\t' | '\r' =>
              offset += 1

            case '[' =>
              if (state == ASYNC_PRESTART) {
                offset += 1
                state = ASYNC_START
              } else if (state == ASYNC_END) {
                if (multiValue) {
                  offset += 1
                  state = ASYNC_START
                } else die(offset, "expected eof")
              } else if (state == ASYNC_POSTVAL)
                die(offset, "expected , or ]")
              else
                state = 0

            case ',' =>
              if (state == ASYNC_POSTVAL) {
                offset += 1
                state = ASYNC_PREVAL
              } else if (state == ASYNC_END)
                die(offset, "expected eof")
              else
                die(offset, "expected json value")

            case ']' =>
              if (state == ASYNC_POSTVAL || state == ASYNC_START)
                if (streamMode > 0) {
                  offset += 1
                  state = ASYNC_END
                } else
                  die(offset, "expected json value or eof")
              else if (state == ASYNC_END)
                die(offset, "expected eof")
              else
                die(offset, "expected json value")

            case c =>
              if (state == ASYNC_END)
                die(offset, "expected eof")
              else if (state == ASYNC_POSTVAL)
                die(offset, "expected ] or ,")
              else {
                if (state == ASYNC_PRESTART && streamMode > 0) streamMode = -1
                state = 0
              }
          }
        else {
          // jump straight back into rparse
          offset = reset(offset)
          val (value, j) =
            if (state <= 0)
              parse(offset)
            else
              rparse(state, curr, context, stack)
          if (streamMode > 0)
            state = ASYNC_POSTVAL
          else if (streamMode == 0)
            state = ASYNC_PREVAL
          else
            state = ASYNC_END
          curr = j
          offset = j
          context = null
          stack = Nil
          results += value
        }
      Right(results)
    } catch {
      case e: AsyncException =>
        if (done)
          // if we are done, make sure we ended at a good stopping point
          if (state == ASYNC_PREVAL || state == ASYNC_END) Right(results)
          else Left(ParseException("exhausted input", -1, -1, -1))
        else
          // we ran out of data, so return what we have so far
          Right(results)

      case e: ParseException =>
        // we hit a parser error, so return that error and results so far
        Left(e)
    }
  }

  // every 1M we shift our array back to the beginning.
  final protected[this] def reset(i: Int): Int =
    if (offset >= 1048576) {
      val diff = offset
      curr -= diff
      len -= diff
      offset = 0
      pos -= diff
      System.arraycopy(data, diff, data, 0, len)
      i - diff
    } else
      i

  /**
   * We use this to keep track of the last recoverable place we've seen. If we hit an AsyncException, we can later
   * resume from this point.
   *
   * This method is called during every loop of rparse, and the arguments are the exact arguments we can pass to rparse
   * to continue where we left off.
   */
  final protected[this] def checkpoint(
    state: Int,
    i: Int,
    context: FContext[J],
    stack: List[FContext[J]]
  ): Unit = {
    this.state = state
    this.curr = i
    this.context = context
    this.stack = stack
  }

  /**
   * This is a specialized accessor for the case where our underlying data are bytes not chars.
   */
  final protected[this] def byte(i: Int): Byte =
    if (i >= len) throw new AsyncException else data(i)

  // we need to signal if we got out-of-bounds
  final protected[this] def at(i: Int): Char =
    if (i >= len) throw new AsyncException else data(i).toChar

  /**
   * Access a byte range as a string.
   *
   * Since the underlying data are UTF-8 encoded, i and k must occur on unicode boundaries. Also, the resulting String
   * is not guaranteed to have length (k - i).
   */
  final protected[this] def at(i: Int, k: Int): CharSequence = {
    if (k > len) throw new AsyncException
    val size = k - i
    val arr = new Array[Byte](size)
    System.arraycopy(data, i, arr, 0, size)
    new String(arr, utf8)
  }

  // the basic idea is that we don't signal EOF until done is true, which means
  // the client explicitly send us an EOF.
  final protected[this] def atEof(i: Int): Boolean =
    if (done) i >= len else false

  // we don't have to do anything special on close.
  final protected[this] def close(): Unit = ()
}

/**
 * This class is used internally by AsyncParser to signal that we've reached the end of the particular input we were
 * given.
 */
private[jawn] class AsyncException extends Exception with control.NoStackTrace

/**
 * This is a more prosaic exception which indicates that we've hit a parsing error.
 */
private[jawn] class FailureException extends Exception
