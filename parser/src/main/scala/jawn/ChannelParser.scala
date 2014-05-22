package jawn

import java.io.{File, FileInputStream}
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

object ChannelParser {
  def fromFile[J](f: File): ChannelParser[J] =
    new ChannelParser[J](new FileInputStream(f).getChannel)
}

// TODO: it is possible that the buffering machinery here is too
// heavyweight. also, i think there may be rare cases where a
// super-long string could fail to parse. consider other options.

/**
 * Basic file parser.
 *
 * Given a file name this parser opens it, chunks the data 256K at a
 * time, and parses it.
 */
final class ChannelParser[J](ch: ReadableByteChannel) extends SyncParser[J] with ByteBasedParser[J] {

  // 256K buffers: arrived at via a bit of testing
  @inline final def bufsize = 262144
  @inline final def mask = bufsize - 1

  // these are the actual byte arrays we'll use
  private var curr = new Array[Byte](bufsize)
  private var next = new Array[Byte](bufsize)

  // these are the bytebuffers used to load the data
  private var bcurr = ByteBuffer.wrap(curr)
  private var bnext = ByteBuffer.wrap(next)

  // these are the bytecounts for each array
  private var ncurr = ch.read(bcurr)
  private var nnext = ch.read(bnext)

  var line = 0
  private var pos = 0
  protected[this] final def newline(i: Int) { line += 1; pos = i }
  protected[this] final def column(i: Int) = i - pos

  protected[this] final def close(): Unit = ch.close()

  /**
   * Swap the curr and next arrays/buffers/counts.
   *
   * We'll call this in response to certain reset() calls. Specifically, when
   * the index provided to reset is no longer in the 'curr' buffer, we want to
   * clear that data and swap the buffers.
   */
  protected[this] final def swap(): Unit = {
    var tmp = curr; curr = next; next = tmp
    var btmp = bcurr; bcurr = bnext; bnext = btmp
    var ntmp = ncurr; ncurr = nnext; nnext = ntmp
  }

  /**
   * If the cursor 'i' is past the 'curr' buffer, we want to clear the
   * current byte buffer, do a swap, load some more data, and
   * continue.
   */
  protected[this] final def reset(i: Int): Int = {
    if (i >= bufsize) {
      bcurr.clear()
      swap()
      nnext = ch.read(bnext)
      pos -= bufsize
      i - bufsize
    } else {
      i
    }
  }

  protected[this] final def checkpoint(state: Int, i: Int, stack: List[FContext[J]]): Unit = ()

  /**
   * This is a specialized accessor for the case where our underlying
   * data are bytes not chars.
   */
  protected[this] final def byte(i: Int): Byte =
    if (i < bufsize) curr(i) else next(i & mask)

  /**
   * Reads a byte as a single Char. The byte must be valid ASCII (this
   * method is used to parse JSON values like numbers, constants, or
   * delimiters, which are known to be within ASCII).
   */
  protected[this] final def at(i: Int): Char =
    if (i < bufsize) curr(i).toChar else next(i & mask).toChar

  /**
   * Access a byte range as a string.
   *
   * Since the underlying data are UTF-8 encoded, i and k must occur
   * on unicode boundaries. Also, the resulting String is not
   * guaranteed to have length (k - i).
   */
  protected[this] final def at(i: Int, k: Int): String = {
    val len = k - i

    if (k <= bufsize) {
      new String(curr, i, len, utf8)
    } else {
      val arr = new Array[Byte](len)
      val mid = bufsize - i
      System.arraycopy(curr, i, arr, 0, mid)
      System.arraycopy(next, 0, arr, mid, k - bufsize)
      new String(arr, utf8)
    }
  }

  protected[this] final def atEof(i: Int) =
    if (i < bufsize) i >= ncurr else (i - bufsize) >= nnext
}
