package jawn

import scala.annotation.{switch, tailrec}

import java.io.FileInputStream
import java.nio.ByteBuffer

trait Parser {

  // states
  @inline final val DAT = 1
  @inline final val KEY = 2
  @inline final val SEP = 3
  @inline final val ARR = 4
  @inline final val OBJ = 5

  def reset(i: Int): Unit
  def die(i: Int, msg: String) = sys.error("%s got %s (%d)" format (msg, at(i), i))

  def all: String
  def at(i: Int): Char
  def at(i: Int, j: Int): String
  def atEof(i: Int): Boolean
  def is(i: Int, c: Char): Boolean = at(i) == c
  def is(i: Int, j: Int, str: String): Boolean = at(i, j) == str

  // this code relies on parseLong/parseDouble to blow up for invalid inputs;
  // it does not try to exactly model the JSON input because we're not actually
  // going to "build" the numbers ourselves. it just needs to be sure that for
  // valid JSON we will find the right "number region".
  def parseNum(i: Int): (Value, Int) = {
    var j = i
    var integral = true
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
      (DoubleNum(java.lang.Double.parseDouble(at(i, j))), j)
    } else if (j - i < 19) {
      (LongNum(java.lang.Long.parseLong(at(i, j), 10)), j)
    } else {
      (DoubleNum(java.lang.Double.parseDouble(at(i, j))), j)
    }
  }
  
  // used to parse the 4 hex digits from "\u1234" (i.e. "1234")
  final def descape(s: String) = java.lang.Integer.parseInt(s, 16).toChar

  // TODO: try using debox.buffer.Mutable + new String(arr, i, len)
  // instead of StringBuilder
  final def parseString(i: Int): (String, Int) = {
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
      c = at(j)
    }
    (sb.toString, j + 1)
  }

  def parseTrue(i: Int) =
    if (is(i, i + 4, "true")) True else die(i, "expected true")

  def parseFalse(i: Int) =
    if (is(i, i + 5, "false")) False else die(i, "expected false")

  def parseNull(i: Int) =
    if (is(i, i + 4, "null")) Null else die(i, "expected null")

  def parse(i: Int): Value = (at(i): @switch) match {
    case ' ' => parse(i + 1)
    case '\t' => parse(i + 1)
    case '\n' => parse(i + 1)

    case '[' => rparse(DAT, i + 1, new ArrContext :: Nil)
    case '{' => rparse(KEY, i + 1, new ObjContext :: Nil)

    case '-' | '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' =>
      try {
        LongNum(java.lang.Long.parseLong(all))
      } catch {
        case e:NumberFormatException =>
          DoubleNum(java.lang.Double.parseDouble(all))
      }

    case '"' =>
      val (str, j) = parseString(i)
      if (atEof(j)) Str(str) else die(j, "expected eof")

    case 't' => if (atEof(i + 4)) parseTrue(i) else die(i + 4, "expected eof")
    case 'f' => if (atEof(i + 5)) parseFalse(i) else die(i + 5, "expected eof")
    case 'n' => if (atEof(i + 4)) parseNull(i) else die(i + 4, "expected eof")

    case _ => die(i, "expected json value")
  }

  // TODO test c then state to unify whitespace handling :P
  @tailrec
  final def rparse(state: Int, i: Int, stack: List[Context]): Container = {
    reset(i)
    (state: @switch) match {
      case DAT => (at(i): @switch) match {
        case ' ' => rparse(state, i + 1, stack)
        case '\t' => rparse(state, i + 1, stack)
        case '\n' => rparse(state, i + 1, stack)
        case '[' => rparse(DAT, i + 1, new ArrContext :: stack)
        case '{' => rparse(KEY, i + 1, new ObjContext :: stack)

        case ']' => stack match {
          case (ctxt1:ArrContext) :: Nil =>
            ctxt1.finish
          case (ctxt1:ArrContext) :: ctxt2 :: tail =>
            ctxt2.add(ctxt1.finish)
            rparse(if (ctxt2.isObj) OBJ else ARR, i + 1, ctxt2 :: tail)
          case _ =>
            sys.error("invalid stack")
        }

        case '-' | '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' =>
          val (n, j) = parseNum(i)
          val ctxt = stack.head
          ctxt.add(n)
          rparse(if (ctxt.isObj) OBJ else ARR, j, stack)

        case '"' =>
          val (str, j) = parseString(i)
          val ctxt = stack.head
          ctxt.add(Str(str))
          rparse(if (ctxt.isObj) OBJ else ARR, j, stack)

        case 't' =>
          val ctxt = stack.head
          ctxt.add(parseTrue(i))
          rparse(if (ctxt.isObj) OBJ else ARR, i + 4, stack)

        case 'f' =>
          val ctxt = stack.head
          ctxt.add(parseFalse(i))
          rparse(if (ctxt.isObj) OBJ else ARR, i + 5, stack)

        case 'n' =>
          val ctxt = stack.head
          ctxt.add(parseNull(i))
          rparse(if (ctxt.isObj) OBJ else ARR, i + 4, stack)
      }

      case KEY => (at(i): @switch) match {
        case ' ' => rparse(state, i + 1, stack)
        case '\t' => rparse(state, i + 1, stack)
        case '\n' => rparse(state, i + 1, stack)

        case '"' =>
          val (str, j) = parseString(i)
          stack.head.asInstanceOf[ObjContext].addKey(str)
          rparse(SEP, j, stack)

        case _ => die(i, "expected \"")
      }

      case SEP => (at(i): @switch) match {
        case ' ' => rparse(state, i + 1, stack)
        case '\t' => rparse(state, i + 1, stack)
        case '\n' => rparse(state, i + 1, stack)

        case ':' => rparse(DAT, i + 1, stack)

        case _ => die(i, "expected :")
      }

      case ARR => (at(i): @switch) match {
        case ' ' => rparse(state, i + 1, stack)
        case '\t' => rparse(state, i + 1, stack)
        case '\n' => rparse(state, i + 1, stack)

        case ',' => rparse(DAT, i + 1, stack)

        case ']' => stack match {
          case ctxt1 :: Nil =>
            ctxt1.finish
          case ctxt1 :: ctxt2 :: tail =>
            ctxt2.add(ctxt1.finish)
            rparse(if (ctxt2.isObj) OBJ else ARR, i + 1, ctxt2 :: tail)
          case _ =>
            sys.error("invalid stack")
        }

        case _ => die(i, "expected ] or ,")
      }

      case OBJ => (at(i): @switch) match {
        case ' ' => rparse(state, i + 1, stack)
        case '\t' => rparse(state, i + 1, stack)
        case '\n' => rparse(state, i + 1, stack)

        case ',' => rparse(KEY, i + 1, stack)

        case '}' => stack match {
          case ctxt1 :: Nil =>
            ctxt1.finish
          case ctxt1 :: ctxt2 :: tail =>
            ctxt2.add(ctxt1.finish)
            rparse(if (ctxt2.isObj) OBJ else ARR, i + 1, ctxt2 :: tail)
          case _ =>
            sys.error("invalid stack")
        }

        case _ => die(i, "expected } or ,")
      }
    }
  }
}

object Parser {
  def parse(s: String): Value = new StringParser(s).parse(0)
}

final class StringParser(s: String) extends Parser {
  def reset(i: Int): Unit = ()
  def at(i: Int): Char = s.charAt(i)
  def at(i: Int, j: Int): String = s.substring(i, j)
  def atEof(i: Int) = i == s.length
  def all = s
}

// FIXME: not quite there yet
final class PathParser(name: String) extends Parser {
  @inline final def bufsize = 1024
  @inline final def shift = 10
  //@inline final def bufsize = 131072 // 128k blocks
  //@inline final def shift = 17 // x / 131072 == x >> 17
  @inline final def mask = bufsize - 1
  @inline final def numbufs = 4

  val f = new FileInputStream(name)
  val ch = f.getChannel()

  var pos = 0
  var n = 1

  val cs = Array.fill(numbufs)(new Array[Byte](bufsize))
  val bs = cs.map(ByteBuffer.wrap(_))
  val ns = Array.fill(numbufs)(-1)

  (0 until numbufs).foreach(init)

  private def unset(j: Int) = ns(j) == -1

  private def init(j: Int) {
    ns(j) = ch.read(bs(j))
    if (ns(j) == -1) {
      bs(j) = null
      cs(j) = null
    }
  }

  private def free(j: Int) {
    bs(j).clear()
    ns(j) = -1
  }

  def reset(j: Int): Unit = {
    val k = (j >> shift) % 4
    if (k > pos) {
      while (pos < k) {
        free(pos)
        init(pos)
        pos += 1
      }
    }
  }

  def at(i: Int): Char = cs((i >> shift) % 4)(i & mask).toChar
  def at(i: Int, k: Int): String = {
    val ishift = (i >> shift) % 4
    val kshift = (k >> shift) % 4
    if (ishift == kshift) {
      // this is the common case where the string comes from one sub-array
      new String(cs(ishift), i & mask, k - i)
    } else {
      // this is the case where our string crosses sub-array boundaries, so
      // we'll need to allocate it ourselves.
      val imask = i & mask

      val arr = new Array[Byte](k - i)
      var sz = bufsize - imask
      System.arraycopy(cs(ishift), imask, arr, 0, sz)

      var jshift = ishift + 1 
      while (jshift < kshift) {
        System.arraycopy(cs(jshift), 0, arr, sz, bufsize)
        sz += bufsize
        jshift += 1
      }

      System.arraycopy(cs(kshift), 0, arr, sz, k & mask)
      new String(arr)
    }
  }

  def atEof(i: Int) = ns(i >> shift) == -1
  def all = sys.error("fixme")
}
