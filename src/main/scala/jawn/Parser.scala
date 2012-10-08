package jawn

import scala.annotation.{switch, tailrec}

import java.lang.Character.isHighSurrogate
import java.lang.Double.parseDouble
import java.lang.Integer.parseInt
import java.lang.Long.parseLong

import java.io.FileInputStream
import java.nio.ByteBuffer

import debox.buffer.Mutable


/**
 * Parser contains the state machine that does all the work. The only 
 */
trait Parser {

  /**
   * Read all remaining data from 'i' onwards and return it as a String.
   *
   * This is only used in the case where we have a top-level number, so it's
   * not generally used in the "critical path", since there's an upper-bound
   * to how long a single number literal can be.
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
  final def is(i: Int, c: Char): Boolean = at(i) == c

  /**
   * Return true iff the bytes/chars from 'i' until 'j' are equal to 'str'.
   */
  final def is(i: Int, j: Int, str: String): Boolean = at(i, j) == str

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
   * TODO: We spend a *lot* of time in parseDouble() so
   * consider other possible alternatives.
   */
  final def parseNum(i: Int, ctxt: Context): Int = {
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
      ctxt.add(DeferNum(at(i, j)))
      j
    } else if (j - i < 19) {
      ctxt.add(DeferNum(at(i, j)))
      j
    } else {
      ctxt.add(DeferNum(at(i, j)))
      j
    }
  }

  /**
   * Generate a Char from the hex digits of "\u1234" (i.e. "1234").
   *
   * NOTE: This is only capable of generating characters from the basic plane.
   * This is why it can only return Char instead of Int.
   */
  final def descape(s: String) = parseInt(s, 16).toChar

  /**
   * Parse the JSON string starting at 'i' and save it into 'ctxt'.
   */
  def parseString(i: Int, ctxt: Context): Int

  /**
   * Parse the JSON constant "true".
   */
  final def parseTrue(i: Int) =
    if (is(i, i + 4, "true")) True else die(i, "expected true")

  /**
   * Parse the JSON constant "false".
   */
  final def parseFalse(i: Int) =
    if (is(i, i + 5, "false")) False else die(i, "expected false")

  /**
   * Parse the JSON constant "null".
   */
  final def parseNull(i: Int) =
    if (is(i, i + 4, "null")) Null else die(i, "expected null")

  /**
   * Parse the JSON document into a single JSON value.
   *
   * The parser considers documents like '333', 'true', and '"foo"' to be
   * valid, as well as more traditional documents like [1,2,3,4,5]. However,
   * multiple top-level objects are not allowed.
   */
  final def parse(i: Int): Value = (at(i): @switch) match {
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
      DeferNum(all(i))

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
  final def reset(i: Int): Int = i
  final def at(i: Int): Char = s.charAt(i)
  final def at(i: Int, j: Int): String = s.substring(i, j)
  final def atEof(i: Int) = i == s.length
  final def all(i: Int) = s.substring(i)

  /**
   * See if the string has any escape sequences. If not, return the end of the
   * string. If so, bail out and return -1.
   *
   * This method expects the data to be in UTF-16 and accesses it as chars.
   * In a few cases we might bail out incorrectly (by reading the second-half
   * of a surrogate pair as \\) but for now the belief is that checking every
   * character would be more expensive. So... in those cases we'll fall back to
   * the slower (correct) UTF-16 parsing.
   */
  final def parseStringSimple(i: Int, ctxt: Context): Int = {
    var j = i
    var c = at(j)
    while (c != '"') {
      if (c == '\\') return -1
      j += 1
      c = at(j)
    }
    j + 1
  }

  /**
   * Parse the string according to JSON rules, and add to the given context.
   *
   * This method expects the data to be in UTF-16, and access it as Char. It
   * performs the correct checks to make sure that we don't interpret a
   * multi-char code point incorrectly.
   */
  final def parseString(i: Int, ctxt: Context): Int = {
    if (at(i) != '"') sys.error("argh")
    var j = i + 1

    val k = parseStringSimple(j, ctxt)
    if (k != -1) {
      ctxt.add(at(i + 1, k - 1))
      return k
    }

    val sb = new CharBuilder
      
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
      } else if (isHighSurrogate(c)) {
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
    ctxt.add(sb.makeString)
    j + 1
  }
}

/**
 * Basic file parser.
 *
 * Given a file name this parser opens it, chunks the data 1M at a time, and
 * parses it. 
 */
final class PathParser(name: String) extends Parser {
  val d = java.nio.charset.Charset.defaultCharset
  if (d.displayName != "UTF-8")
    sys.error("default encoding must be UTF-8, got %s." format d)

  // TODO: figure out if we wouldn't just be better off decoding to UTF-16 on
  // input, rather than doing the parsing as UTF-8. i'm not sure which works
  // better since we'll save memory this way but add a bit of complexity. so
  // far it's working but more resarch is needed.
  //
  // on the upside, we avoid a lot of annoying ceremony around Charset,
  // CharBuffer, array-copying, and so on.
  //
  // on the downside, we don't support other encodings, and it's not clear how
  // much time we actually spend doing any of this relative to the actual
  // parsing.

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
  final def swap() {
    var tmp = curr; curr = next; next = tmp
    var btmp = bcurr; bcurr = bnext; bnext = btmp
    var ntmp = ncurr; ncurr = nnext; nnext = ntmp
  }

  /**
   * If the cursor 'i' is past the 'curr' buffer, we want to clear the current
   * byte buffer, do a swap, load some more data, and continue.
   */
  final def reset(i: Int): Int = {
    if (i >= bufsize) {
      bcurr.clear()
      swap()
      nnext = ch.read(bnext)
      i - bufsize
    } else {
      i
    }
  }

  /**
   * This is a specialized accessor for the case where our underlying data are
   * bytes not chars.
   */
  final def byte(i: Int): Byte = if (i < bufsize)
    curr(i)
  else
    next(i & mask)

  /**
   * Rads
   */
  final def at(i: Int): Char = if (i < bufsize)
    curr(i).toChar
  else
    next(i & mask).toChar

  /**
   * Access a byte range as a string.
   *
   * Since the underlying data are UTF-8 encoded, i and k must occur on unicode
   * boundaries. Also, the resulting String is not guaranteed to have length
   * (k - i).
   */
  final def at(i: Int, k: Int): String = {
    val len = k - i

    if (k <= bufsize) {
      new String(curr, i, len)
    } else {
      val arr = new Array[Byte](len)
      val mid = bufsize - i
      System.arraycopy(curr, i, arr, 0, mid)
      System.arraycopy(next, 0, arr, mid, k - bufsize)
      new String(arr)
    }
  }

  final def atEof(i: Int) = if (i < bufsize) i >= ncurr else i >= nnext

  final def all(i: Int) = {
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

  /**
   * See if the string has any escape sequences. If not, return the end of the
   * string. If so, bail out and return -1.
   *
   * This method expects the data to be in UTF-8 and accesses it as bytes. Thus
   * we can just ignore any bytes with the highest bit set.
   */
  final def parseStringSimple(i: Int, ctxt: Context): Int = {
    var j = i
    var c = byte(j)
    while (c != 34) {
      if (c == 92) return -1
      j += 1
      c = byte(j)
    }
    j + 1
  }

  /**
   * Parse the string according to JSON rules, and add to the given context.
   *
   * This method expects the data to be in UTF-8 and accesses it as bytes.
   */
  final def parseString(i: Int, ctxt: Context): Int = {
    if (at(i) != 34) sys.error("argh")
    var j = i + 1

    val k = parseStringSimple(j, ctxt)
    if (k != -1) {
      ctxt.add(at(i + 1, k - 1))
      return k
    }

    val sb = new CharBuilder
      
    var c = byte(j)
    while (c != 34) { // "
      if (c == 92) { // \
        (byte(j + 1): @switch) match {
          case 98 => { sb.append('\b'); j += 2 }
          case 102 => { sb.append('\f'); j += 2 }
          case 110 => { sb.append('\n'); j += 2 }
          case 114 => { sb.append('\r'); j += 2 }
          case 116 => { sb.append('\t'); j += 2 }

          // if there's a problem then descape will explode
          case 117 => { sb.append(descape(at(j + 2, j + 6))); j += 6 }

          // permissive: let any escaped char through, not just ", / and \
          case c2 => { sb.append(c2.toChar); j += 2 }
        }
      } else if (c < 128) {
        // 1-byte UTF-8 sequence
        sb.append(c.toChar)
        j += 1
      } else if ((c & 224) == 192) {
        // 2-byte UTF-8 sequence
        sb.extend(at(j, j + 2))
        j += 2
      } else if ((c & 240) == 224) {
        // 3-byte UTF-8 sequence
        sb.extend(at(j, j + 3))
        j += 3
      } else if ((c & 248) == 240) {
        // 4-byte UTF-8 sequence
        sb.extend(at(j, j + 4))
        j += 4
      } else {
        sys.error("invalid UTF-8 encoding")
      }
      j = reset(j)
      c = byte(j)
    }
    ctxt.add(sb.makeString)
    j + 1
  }
}
