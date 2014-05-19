package jawn

import scala.collection.mutable
import scala.annotation.switch

sealed trait JValue { def j: String }

sealed trait Atom extends JValue

case object JTrue extends Atom { final def j = "true" }
case object JFalse extends Atom { final def j = "false" }
case object JNull extends Atom { final def j = "null" }

case class JString(s: String) extends Atom {
  def j = Str.escape(s)
}

object Str {
  def escape(s: String): String = {
    val sb = new StringBuilder
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
          if (c < ' ')
            sb.append("\\u%04x" format c.toInt)
          else
            sb.append(c)
      }
      i += 1
    }
    sb.append('"')
    sb.toString
  }
}

sealed trait Num extends Atom

case class LongNum(n: Long) extends Num {
  def j = n.toString
  override def equals(that: Any): Boolean = that match {
    case LongNum(n2) => n == n2
    case DoubleNum(n2) => n == n2
    case DeferNum(s) => n.toString == s
  }
}

case class DoubleNum(n: Double) extends Num {
  def j = n.toString
  override def equals(that: Any): Boolean = that match {
    case LongNum(n2) => n == n2
    case DoubleNum(n2) => n == n2
    case DeferNum(s) => n.toString == s
  }
}

case class DeferNum(s: String) extends Num {
  def j = s
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
  def j = if (vs.length == 0) {
    "[]"
  } else {
    val sb = new StringBuilder().append("[")
    vs.foreach(v => sb.append(v.j).append(", "))
    sb.dropRight(2).append("]").toString
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
  def j = if (vs.size == 0) {
    "{}"
  } else {
    val sb = new StringBuilder().append("{")
    vs.foreach { case (k, v) =>
      sb.append("\"" + k + "\": ").append(v.j).append(", ")
    }
    sb.dropRight(2).append("}").toString
  }

  override def equals(that: Any): Boolean = that match {
    case JObject(vs2) => vs == vs2
    case _ => false
  }
}

object JObject {
  def fromSeq(js: Seq[(String, JValue)]): JObject = JObject(mutable.Map(js: _*))
}
