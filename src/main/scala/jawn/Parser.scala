package jawn

import scala.annotation.{switch, tailrec}

import java.io.FileInputStream
import java.nio.ByteBuffer

import debox.buffer.Mutable

/**
 * Parser contains the state machine that does all the work. The only 
 */
trait Parser {

  /**
   * Read all remaining data from 'i' onwards and return it as a String.
   */
  def all(i: Int): String

  /**
   * Read the byte/char at 'i' as a Char.
   *
   * Note that this should not be used on potential multi-byte sequences.
   */
  def at(i: Int): Char

  /**
   * Read the bytes/chars from 'i' until 'j' as a String.
   */
  def at(i: Int, j: Int): String

  /**
   * Return true iff 'i' is at or beyond the end of the input (EOF).
   */
  def atEof(i: Int): Boolean

  /**
   * Return true iff the byte/char at 'i' is equal to 'c'.
   */
  def is(i: Int, c: Char): Boolean = at(i) == c

  /**
   * Return true iff the bytes/chars from 'i' until 'j' are equal to 'str'.
   */
  def is(i: Int, j: Int, str: String): Boolean = at(i, j) == str

  /**
   * The reset() method is used to signal that we're working from the given
   * position, and any previous data can be released. Some parsers (e.g.
   * StringParser) will ignore release, while others (e.g. PathParser) will
   * need to use this information to release and allocate different areas.
   */
  def reset(i: Int): Int

  /**
   * Valid parser states.
   */
  @inline final val ARRBEG = 6
  @inline final val OBJBEG = 7
  @inline final val DATA = 1
  @inline final val KEY = 2
  @inline final val SEP = 3
  @inline final val ARREND = 4
  @inline final val OBJEND = 5

  /**
   * Used to generate error messages with character info and byte addresses.
   */
  protected[this] def die(i: Int, msg: String) =
    sys.error("%s got %s (%d)" format (msg, at(i), i))

  /**
   * Parse the given number, and add it to the given context.
   *
   * This code relies on parseLong/parseDouble to blow up for invalid inputs;
   * it does not try to exactly model the JSON input because we're not actually
   * going to "build" the numbers ourselves. It just needs to be sure that for
   * valid JSON we will find the right "number region".
   *
   * TODO: We spend a *lot* of time in java.lang.Double.parseDouble() so
   * consider other possible alternatives.
   */
  def parseNum(i: Int, ctxt: Context): Int = {
    var j = i
    var c = at(j)

    if (c == '-') { j += 1; c = at(j) }
    while ('0' <= c && c <= '9') { j += 1; c = at(j) }

    if (c == '.' || c == 'e' || c == 'E') {
      j += 1
      c = at(j)
      while ('0' <= c && c <= '9' || c == '+' || c == '-' || c == 'e' || c == 'E') {
        j += 1
        c = at(j)
      }
      ctxt.add(DoubleNum(java.lang.Double.parseDouble(at(i, j))))
      j
    } else if (j - i < 19) {
      ctxt.add(LongNum(java.lang.Long.parseLong(at(i, j), 10)))
      j
    } else {
      ctxt.add(DoubleNum(java.lang.Double.parseDouble(at(i, j))))
      j
    }
  }

  /**
   * Generate a Char from the hex digits of "\u1234" (i.e. "1234").
   *
   * NOTE: This is only capable of generating characters from the basic plane.
   * This is why it can only return Char instead of Int.
   */
  final def descape(s: String) = java.lang.Integer.parseInt(s, 16).toChar

  /**
   * Parse the string according to JSON rules, and add to the given context.
   *
   * TODO: Make sure we're handling encodings (i.e. UTF-8) correctly.
   *
   * TODO: See if debox.buffer.Mutable with new String(arr, i, len) is faster.
   */
  final def parseString(i: Int, ctxt: Context): Int = {
    if (at(i) != '"') sys.error("argh")
    val sb = new StringBuilder
    var j = i + 1
    var c = at(j)
    while (c != '"') {
      if (c == '\\') {
        (at(j + 1): @switch) match {
          case 'b' => { sb.append('\b'); j += 2 }
          case 'f' => { sb.append('\f'); j += 2 }
          case 'n' => { sb.append('\n'); j += 2 }
          case 'r' => { sb.append('\r'); j += 2 }
          case 't' => { sb.append('\t'); j += 2 }

          // if there's a problem then descape will explode
          case 'u' => { sb.append(descape(at(j + 2, j + 6))); j += 6 }

          // permissive: let any escaped char through, not just ", / and \
          case c2 => { sb.append(c2); j += 2 }
        }
      } else if (java.lang.Character.isHighSurrogate(c)) {
        // this case dodges the situation where we might incorrectly parse the
        // second Char of a unicode code point.
        sb.append(c)
        sb.append(at(j + 1))
        j += 2
      } else {
        // this case is for "normal" code points that are just one Char.
        sb.append(c)
        j += 1
      }
      j = reset(j)
      c = at(j)
    }
    ctxt.add(sb.toString)
    j + 1
  }

  /**
   * Parse the JSON constant "true".
   */
  def parseTrue(i: Int) =
    if (is(i, i + 4, "true")) True else die(i, "expected true")

  /**
   * Parse the JSON constant "false".
   */
  def parseFalse(i: Int) =
    if (is(i, i + 5, "false")) False else die(i, "expected false")

  /**
   * Parse the JSON constant "null".
   */
  def parseNull(i: Int) =
    if (is(i, i + 4, "null")) Null else die(i, "expected null")

  /**
   * Parse the JSON document into a single JSON value.
   *
   * The parser considers documents like '333', 'true', and '"foo"' to be
   * valid, as well as more traditional documents like [1,2,3,4,5]. However,
   * multiple top-level objects are not allowed.
   */
  def parse(i: Int): Value = (at(i): @switch) match {
    // ignore whitespace
    case ' ' => parse(i + 1)
    case '\t' => parse(i + 1)
    case '\n' => parse(i + 1)

    // if we have a recursive top-level structure, we'll delegate the parsing
    // duties to our good friend rparse().
    case '[' => rparse(ARRBEG, i + 1, new ArrContext :: Nil)
    case '{' => rparse(OBJBEG, i + 1, new ObjContext :: Nil)

    // we have a single top-level number
    case '-' | '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' =>
      try {
        LongNum(java.lang.Long.parseLong(all(i)))
      } catch {
        case e:NumberFormatException =>
          DoubleNum(java.lang.Double.parseDouble(all(i)))
      }

    // we have a single top-level string
    case '"' =>
      val ctxt = new ArrContext
      val j = parseString(i, ctxt)
      if (atEof(j)) ctxt.finish.vs(0) else die(j, "expected eof")

    // we have a single top-level constant
    case 't' => if (atEof(i + 4)) parseTrue(i) else die(i + 4, "expected eof")
    case 'f' => if (atEof(i + 5)) parseFalse(i) else die(i + 5, "expected eof")
    case 'n' => if (atEof(i + 4)) parseNull(i) else die(i + 4, "expected eof")

    // invalid
    case _ =>
      die(i, "expected json value")
  }

  /**
   * Tail-recursive parsing method to do the bulk of JSON parsing.
   *
   * This single method manages parser states, data, etc. Except for parsing
   * non-recursive values (like strings, numbers, and constants) all important
   * work happens in this loop (or in methods it calls, like reset()).
   *
   * Currently the code is optimized to make use of switch statements. Future
   * work should consider whether this is better or worse than manually
   * constructed if/else statements or something else.
   */
  @tailrec
  final def rparse(state: Int, j: Int, stack: List[Context]): Container = {
    val i = reset(j)
    (state: @switch) match {
      // we are inside an object or array expecting to see data
      case DATA => (at(i): @switch) match {
        case ' ' => rparse(state, i + 1, stack)
        case '\t' => rparse(state, i + 1, stack)
        case '\n' => rparse(state, i + 1, stack)

        case '[' => rparse(ARRBEG, i + 1, new ArrContext :: stack)
        case '{' => rparse(OBJBEG, i + 1, new ObjContext :: stack)

        case '-' | '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' =>
          val ctxt = stack.head
          val j = parseNum(i, ctxt)
          rparse(if (ctxt.isObj) OBJEND else ARREND, j, stack)

        case '"' =>
          val ctxt = stack.head
          val j = parseString(i, ctxt)
          rparse(if (ctxt.isObj) OBJEND else ARREND, j, stack)

        case 't' =>
          val ctxt = stack.head
          ctxt.add(parseTrue(i))
          rparse(if (ctxt.isObj) OBJEND else ARREND, i + 4, stack)

        case 'f' =>
          val ctxt = stack.head
          ctxt.add(parseFalse(i))
          rparse(if (ctxt.isObj) OBJEND else ARREND, i + 5, stack)

        case 'n' =>
          val ctxt = stack.head
          ctxt.add(parseNull(i))
          rparse(if (ctxt.isObj) OBJEND else ARREND, i + 4, stack)
      }

      // we are in an object expecting to see a key
      case KEY => (at(i): @switch) match {
        case ' ' => rparse(state, i + 1, stack)
        case '\t' => rparse(state, i + 1, stack)
        case '\n' => rparse(state, i + 1, stack)

        case '"' =>
          val j = parseString(i, stack.head)
          rparse(SEP, j, stack)

        case _ => die(i, "expected \"")
      }

      // we are starting an array, expecting to see data or a closing bracket
      case ARRBEG => (at(i): @switch) match {
        case ' ' => rparse(state, i + 1, stack)
        case '\t' => rparse(state, i + 1, stack)
        case '\n' => rparse(state, i + 1, stack)

        case ']' => stack match {
          case ctxt1 :: Nil =>
            ctxt1.finish
          case ctxt1 :: ctxt2 :: tail =>
            ctxt2.add(ctxt1.finish)
            rparse(if (ctxt2.isObj) OBJEND else ARREND, i + 1, ctxt2 :: tail)
          case _ =>
            sys.error("invalid stack")
        }

        case _ => rparse(DATA, i, stack)
      }

      // we are starting an object, expecting to see a key or a closing brace
      case OBJBEG => (at(i): @switch) match {
        case ' ' => rparse(state, i + 1, stack)
        case '\t' => rparse(state, i + 1, stack)
        case '\n' => rparse(state, i + 1, stack)

        case '}' => stack match {
          case ctxt1 :: Nil =>
            ctxt1.finish
          case ctxt1 :: ctxt2 :: tail =>
            ctxt2.add(ctxt1.finish)
            rparse(if (ctxt2.isObj) OBJEND else ARREND, i + 1, ctxt2 :: tail)
          case _ =>
            sys.error("invalid stack")
        }

        case _ => rparse(KEY, i, stack)
      }

      // we are in an object just after a key, expecting to see a colon
      case SEP => (at(i): @switch) match {
        case ' ' => rparse(state, i + 1, stack)
        case '\t' => rparse(state, i + 1, stack)
        case '\n' => rparse(state, i + 1, stack)

        case ':' => rparse(DATA, i + 1, stack)

        case _ => die(i, "expected :")
      }

      // we are at a possible stopping point for an array, expecting to see
      // either a comma (before more data) or a closing bracket.
      case ARREND => (at(i): @switch) match {
        case ' ' => rparse(state, i + 1, stack)
        case '\t' => rparse(state, i + 1, stack)
        case '\n' => rparse(state, i + 1, stack)

        case ',' => rparse(DATA, i + 1, stack)

        case ']' => stack match {
          case ctxt1 :: Nil =>
            ctxt1.finish
          case ctxt1 :: ctxt2 :: tail =>
            ctxt2.add(ctxt1.finish)
            rparse(if (ctxt2.isObj) OBJEND else ARREND, i + 1, ctxt2 :: tail)
          case _ =>
            sys.error("invalid stack")
        }

        case _ => die(i, "expected ] or ,")
      }

      // we are at a possible stopping point for an object, expecting to see
      // either a comma (before more data) or a closing brace.
      case OBJEND => (at(i): @switch) match {
        case ' ' => rparse(state, i + 1, stack)
        case '\t' => rparse(state, i + 1, stack)
        case '\n' => rparse(state, i + 1, stack)

        case ',' => rparse(KEY, i + 1, stack)

        case '}' => stack match {
          case ctxt1 :: Nil =>
            ctxt1.finish
          case ctxt1 :: ctxt2 :: tail =>
            ctxt2.add(ctxt1.finish)
            rparse(if (ctxt2.isObj) OBJEND else ARREND, i + 1, ctxt2 :: tail)
          case _ =>
            sys.error("invalid stack")
        }
        
        case _ => die(i, "expected } or ,")
      }
    }
  }
}

/**
 * Parser companion, with convenience methods.
 */
object Parser {
  def parseString(s: String): Value = new StringParser(s).parse(0)
  def parsePath(s: String): Value = new PathParser(s).parse(0)
}

/**
 * Basic in-memory string parsing.
 *
 * This parser is limited to the maximum string size (~2G). Obviously for large
 * JSON documents it's better to avoid using this parser and go straight from
 * disk, to avoid having to load the whole thing into memory at once.
 */
final class StringParser(s: String) extends Parser {
  def reset(i: Int): Int = i
  def at(i: Int): Char = s.charAt(i)
  def at(i: Int, j: Int): String = s.substring(i, j)
  def atEof(i: Int) = i == s.length
  def all(i: Int) = s.substring(i)
}

/**
 * Basic file parser.
 *
 * Given a file name this parser opens it, chunks the data 1M at a time, and
 * parses it. 
 */
final class PathParser(name: String) extends Parser {
  // 256K buffers: arrived at via a bit of testing
  @inline final def bufsize = 262144
  @inline final def mask = bufsize - 1

  // fis and channel are the data source
  val f = new FileInputStream(name)
  val ch = f.getChannel()

  // these are the actual byte arrays we'll use
  var curr = new Array[Byte](bufsize)
  var next = new Array[Byte](bufsize)

  // these are the bytebuffers used to load the data
  var bcurr = ByteBuffer.wrap(curr)
  var bnext = ByteBuffer.wrap(next)

  // these are the bytecounts for each array
  var ncurr = ch.read(bcurr)
  var nnext = ch.read(bnext)
  
  /**
   * Swap the curr and next arrays/buffers/counts.
   *
   * We'll call this in response to certain reset() calls. Specifically, when
   * the index provided to reset is no longer in the 'curr' buffer, we want to
   * clear that data and swap the buffers.
   */
  def swap() {
    var tmp = curr; curr = next; next = tmp
    var btmp = bcurr; bcurr = bnext; bnext = btmp
    var ntmp = ncurr; ncurr = nnext; nnext = ntmp
  }

  /**
   * If the cursor 'i' is past the 'curr' buffer, we want to clear the current
   * byte buffer, do a swap, load some more data, and continue.
   */
  def reset(i: Int): Int = {
    if (i >= bufsize) {
      bcurr.clear()
      swap()
      nnext = ch.read(bnext)
      i - bufsize
    } else {
      i
    }
  }

  def at(i: Int): Char = if (i < bufsize)
    curr(i).toChar
  else
    next(i & mask).toChar

  def at(i: Int, k: Int): String = {
    val len = k - i
    val arr = new Array[Byte](len)

    if (k <= bufsize) {
      System.arraycopy(curr, i, arr, 0, len)
    } else {
      val mid = bufsize - i
      System.arraycopy(curr, i, arr, 0, mid)
      System.arraycopy(next, 0, arr, mid, k - bufsize)
    }
    new String(arr)
  }

  def atEof(i: Int) = if (i < bufsize) i >= ncurr else i >= nnext

  def all(i: Int) = {
    var j = i
    val sb = new StringBuilder
    while (!atEof(j)) {
      if (ncurr == bufsize) {
        sb.append(at(j, bufsize))
        j = reset(bufsize)
      } else {
        sb.append(at(j, ncurr))
        j = reset(ncurr)
      }
    }
    sb.toString
  }
}
