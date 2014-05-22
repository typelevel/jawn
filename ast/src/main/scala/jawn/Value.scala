package jawn

import scala.collection.mutable
import scala.annotation.switch
import scala.util.Sorting

sealed trait JValue {
  def pretty(canonical: Boolean = true, unicode: Boolean = false): String
  def prettyB(sb: StringBuilder, depth: Int, canonical: Boolean, unicode: Boolean): Unit

  def compact(canonical: Boolean = false, unicode: Boolean = false): String
  def compactB(sb: StringBuilder, canonical: Boolean, unicode: Boolean): Unit
}

sealed trait Atom extends JValue {
  def compact(canonical: Boolean = false, unicode: Boolean = false): String = pretty(canonical)
  def compactB(sb: StringBuilder, canonical: Boolean, unicode: Boolean): Unit = sb.append(pretty())
  def prettyB(sb: StringBuilder, depth: Int, canonical: Boolean, unicode: Boolean): Unit = sb.append(pretty())
}

case object JTrue extends Atom {
  final def pretty(canonical: Boolean = true, unicode: Boolean = false): String = "true"
}

case object JFalse extends Atom {
  final def pretty(canonical: Boolean = true, unicode: Boolean = false): String = "false"
}

case object JNull extends Atom {
  final def pretty(canonical: Boolean = true, unicode: Boolean = false): String = "null"
}

case class JString(s: String) extends Atom {
  override def compactB(sb: StringBuilder, canonical: Boolean, unicode: Boolean): Unit =
    JString.escape(sb, s, unicode)
  def pretty(canonical: Boolean = true, unicode: Boolean): String =
    JString.escape(s)
  override def prettyB(sb: StringBuilder, depth: Int, canonical: Boolean, unicode: Boolean): Unit =
    JString.escape(sb, s, unicode)
}

object JString {
  final def escape(s: String, unicode: Boolean = false): String = {
    val sb = new StringBuilder
    escape(sb, s, unicode)
    sb.toString
  }

  final def escape(sb: StringBuilder, s: String, unicode: Boolean): Unit = {
    sb.append('"')
    var i = 0
    val len = s.length
    while (i < len) {
      (s.charAt(i): @switch) match {
        case '"' => sb.append("\\\"")
        case '\\' => sb.append("\\\\")
        case '\b' => sb.append("\\b")
        case '\f' => sb.append("\\f")
        case '\n' => sb.append("\\n")
        case '\r' => sb.append("\\r")
        case '\t' => sb.append("\\t")
        case c =>
          if (c < ' ' || (c > '~' && unicode)) sb.append("\\u%04x" format c.toInt)
          else sb.append(c)
      }
      i += 1
    }
    sb.append('"')
  }

  final def escapeUnicode(s: String): String = {
    val cb = new CharBuilder
    var i = 0
    while (i < s.length) {
      val c = s.charAt(i)
      if (c > '~') cb.extend("\\u%04x" format c.toInt)
      else cb.append(c)
      i += 1
    }
    cb.makeString
  }
}

sealed trait Num extends Atom

case class LongNum(n: Long) extends Num {
  def pretty(canonical: Boolean = true, unicode: Boolean = false): String = n.toString
  override def equals(that: Any): Boolean = that match {
    case LongNum(n2) => n == n2
    case DoubleNum(n2) => n == n2
    case DeferNum(s) => n.toString == s
  }
}

case class DoubleNum(n: Double) extends Num {
  def pretty(canonical: Boolean = true, unicode: Boolean = false): String = n.toString
  override def equals(that: Any): Boolean = that match {
    case LongNum(n2) => n == n2
    case DoubleNum(n2) => n == n2
    case DeferNum(s) => n.toString == s
  }
}

case class DeferNum(s: String) extends Num {
  def pretty(canonical: Boolean = true, unicode: Boolean = false): String = s
  override def equals(that: Any): Boolean = that match {
    case LongNum(n2) => s == n2.toString
    case DoubleNum(n2) => s == n2.toString
    case DeferNum(s2) => s == s2
  }
}

object JNum {
  def apply(s: String): Atom = DeferNum(s)
}

sealed trait Container extends JValue

case class JArray(vs: Array[JValue]) extends Container {

  override def toString: String = pretty(true, true)

  def pretty(canonical: Boolean = true, unicode: Boolean = false): String = {
    val sb = new StringBuilder
    prettyB(sb, 0, canonical, unicode)
    sb.toString
  }

  def prettyB(sb: StringBuilder, depth: Int, canonical: Boolean, unicode: Boolean): Unit =
    if (vs.isEmpty) {
      sb.append("[]")
    } else {
      val depth1 = depth + 1
      val pad1 = "  " * (depth1)
      sb.append("[\n").append(pad1)
      vs(0).prettyB(sb, depth1, canonical, unicode)
      var i = 1
      while (i < vs.length) {
        sb.append(",\n").append(pad1)
        vs(i).prettyB(sb, depth1, canonical, unicode)
        i += 1
      }
      val pad0 = "  " * depth
      sb.append("\n").append(pad0).append("]")
    }

  def compact(canonical: Boolean = false, unicode: Boolean = false): String = {
    val sb = new StringBuilder
    compactB(sb, canonical, unicode)
    sb.toString
  }

  def compactB(sb: StringBuilder, canonical: Boolean, unicode: Boolean): Unit =
    if (vs.isEmpty) {
      sb.append("[]")
    } else {
      sb.append("[")
      vs(0).compactB(sb, canonical, unicode)
      var i = 1
      while (i < vs.length) {
        sb.append(",")
        vs(i).compactB(sb, canonical, unicode)
        i += 1
      }
      sb.append("]")
    }

  override def equals(that: Any): Boolean = that match {
    case JArray(vs2) =>
      if (vs.length != vs2.length) return false
      var i = 0
      while (i < vs.length) {
        if (vs(i) != vs2(i)) return false
        i += 1
      }
      true
    case _ =>
      false
  }
}

case class JObject(vs: mutable.Map[String, JValue]) extends Container {

  override def toString: String = pretty(true, true)

  def pretty(canonical: Boolean = true, unicode: Boolean = false): String = {
    val sb = new StringBuilder
    prettyB(sb, 0, canonical, unicode)
    sb.toString
  }

  def prettyB(sb: StringBuilder, depth: Int, canonical: Boolean, unicode: Boolean): Unit =
    if (vs.isEmpty) {
      sb.append("{}")
    } else {
      val depth1 = depth + 1
      val pad1 = "  " * (depth1)
      val it = if (canonical) {
        val keys = vs.keys.toArray
        Sorting.quickSort(keys)
        keys.iterator.map(k => (k, vs(k)))
      } else {
        vs.iterator
      }
      val (k0, v0) = it.next
      sb.append("{\n").append(pad1)
      JString.escape(sb, k0, unicode)
      sb.append(": ")
      v0.prettyB(sb, depth1, canonical, unicode)
      while (it.hasNext) {
        val (k, v) = it.next
        sb.append(",\n").append(pad1)
        JString.escape(sb, k, unicode)
        sb.append(": ")
        v.prettyB(sb, depth1, canonical, unicode)
      }
      val pad0 = "  " * depth
      sb.append("\n").append(pad0).append("}")
    }

  def compact(canonical: Boolean = false, unicode: Boolean = false): String = {
    val sb = new StringBuilder
    compactB(sb, canonical, unicode)
    sb.toString
  }

  def compactB(sb: StringBuilder, canonical: Boolean, unicode: Boolean): Unit =
    if (vs.isEmpty) {
      sb.append("{}")
    } else {
      val it = vs.iterator
      val (k0, v0) = it.next
      JString.escape(sb, k0, unicode)
      sb.append(":")
      v0.compactB(sb, canonical, unicode)
      while (it.hasNext) {
        val (k, v) = it.next
        sb.append(",")
        JString.escape(sb, k, unicode)
        sb.append(":")
        v.compactB(sb, canonical, unicode)
      }
      sb.append("}")
    }

  override def equals(that: Any): Boolean = that match {
    case JObject(vs2) => vs == vs2
    case _ => false
  }
}

object JObject {
  def fromSeq(js: Seq[(String, JValue)]): JObject = JObject(mutable.Map(js: _*))
}
