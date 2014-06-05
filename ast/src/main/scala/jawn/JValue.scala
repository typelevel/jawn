package jawn
package ast

import scala.collection.mutable
import scala.annotation.switch
import scala.util.Sorting

sealed trait JValue {
  def render(r: Renderer): String = r.render(this)
  override def toString: String = CanonicalRenderer.render(this)
}

sealed trait JAtom extends JValue
sealed trait JContainer extends JValue

case object JTrue extends JAtom
case object JFalse extends JAtom
case object JNull extends JAtom

case class JString(s: String) extends JAtom

sealed trait JNum extends JAtom

object JNum {
  def apply(n: Long): JNum = LongNum(n)
  def apply(n: Double): JNum = DoubleNum(n)
  def apply(s: String): JNum = DeferNum(s)
}

case class LongNum(n: Long) extends JNum {
  override def equals(that: Any): Boolean =
    that match {
      case LongNum(n2) => n == n2
      case DoubleNum(n2) => n == n2
      case DeferNum(s) => n.toString == s
    }
}

case class DoubleNum(n: Double) extends JNum {
  override def equals(that: Any): Boolean =
    that match {
      case LongNum(n2) => n == n2
      case DoubleNum(n2) => n == n2
      case DeferNum(s) => n.toString == s
    }
}

case class DeferNum(s: String) extends JNum {
  override def equals(that: Any): Boolean =
    that match {
      case LongNum(n2) => s == n2.toString
      case DoubleNum(n2) => s == n2.toString
      case DeferNum(s2) => s == s2
    }
}

case class JArray(vs: Array[JValue]) extends JContainer {
  override def equals(that: Any): Boolean =
    that match {
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

case class JObject(vs: mutable.Map[String, JValue]) extends JContainer

object JArray {
  def fromSeq(js: Seq[JValue]): JArray = JArray(js.toArray)
}

object JObject {
  def fromSeq(js: Seq[(String, JValue)]): JObject = JObject(mutable.Map(js: _*))
}
