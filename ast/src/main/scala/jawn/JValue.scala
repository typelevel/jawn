package jawn
package ast

import scala.collection.mutable
import scala.util.hashing.MurmurHash3

import spire.algebra.{BooleanAlgebra, Field, IsReal, Monoid, NRoot, Order}
import spire.std.double._
import spire.syntax.field._
import spire.syntax.isReal._
import spire.syntax.nroot._
import spire.syntax.order._

sealed trait JValue {
  def isNull: Boolean = this == JNull
  def orElse(v: JValue): JValue = if (this == JNull) v else this

  def render(r: Renderer): String = r.render(this)
  override def toString: String = CanonicalRenderer.render(this)
}

sealed trait JAtom extends JValue
sealed trait JContainer extends JValue

case object JNull extends JAtom

sealed trait JBool extends JAtom {
  def toBoolean: Boolean = this == JTrue
}

case object JTrue extends JBool
case object JFalse extends JBool

case class JString(s: String) extends JAtom {
  def +(that: JString): JString = JString(this.s + that.s)
}

sealed trait JNum extends JAtom {
  def toDouble: Double
}

case class LongNum(n: Long) extends JNum {
  def toDouble: Double = n.toDouble
  override def hashCode: Int = n.##
  override def equals(that: Any): Boolean =
    that match {
      case LongNum(n2) => n == n2
      case DoubleNum(n2) => n == n2
      case DeferNum(s) => n.toString == s
      case _ => false
    }
}

case class DoubleNum(n: Double) extends JNum {
  def toDouble: Double = n
  override def hashCode: Int = n.##
  override def equals(that: Any): Boolean =
    that match {
      case LongNum(n2) => n == n2
      case DoubleNum(n2) => n == n2
      case DeferNum(s) => n.toString == s
      case _ => false
    }
}

case class DeferNum(s: String) extends JNum {
  lazy val toDouble: Double = s.toDouble
  override def hashCode: Int = toDouble.##
  override def equals(that: Any): Boolean =
    that match {
      case LongNum(n2) => s == n2.toString
      case DoubleNum(n2) => s == n2.toString
      case DeferNum(s2) => s == s2
      case _ => false
    }
}

case class JArray(vs: Array[JValue]) extends JContainer {
  def get(i: Int): JValue =
    if (0 <= i && i < vs.length) vs(i) else JNull

  override def hashCode: Int = MurmurHash3.arrayHash(vs)

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

case class JObject(vs: mutable.Map[String, JValue]) extends JContainer {
  def get(k: String): JValue = vs.getOrElse(k, JNull)
}

object JValue {
  implicit val monoid = new Monoid[JValue] {
    val id: JValue = JNull
    def op(x: JValue, y: JValue): JValue = {
      if (x == JNull || y == JNull) return JNull

      x match {
        case j1: JNum => y match {
          case j2: JNum => DoubleNum(j1.toDouble + j2.toDouble)
          case _ => JNull
        }
        case j1: JString => y match {
          case j2: JString => j1 + j2
          case _ => JNull
        }
        case j1: JBool => y match {
          case j2: JBool => JBool(j1.toBoolean && j2.toBoolean)
          case _ => JNull
        }
        case JArray(js1) => y match {
          case JArray(js2) => JArray(js1 ++ js2)
          case _ => JNull
        }
        case JObject(js1) => y match {
          case JObject(js2) => JObject(js1 ++ js2)
          case _ => JNull
        }
        case JNull => JNull
      }
    }
  }
}

object JBool {
  val True: JBool = JTrue
  val False: JBool = JFalse

  def apply(b: Boolean): JBool = if (b) JTrue else JFalse

  implicit val booleanAlgebra = new BooleanAlgebra[JBool] {
    def zero: JBool = JFalse
    def one: JBool = JTrue
    def and(x: JBool, y: JBool): JBool = JBool(x.toBoolean && y.toBoolean)
    def complement(x: JBool): JBool = JBool(!x.toBoolean)
    def or(x: JBool, y: JBool): JBool = JBool(x.toBoolean || y.toBoolean)
  }

  implicit val monoid = new Monoid[JBool] {
    def id: JBool = JTrue
    def op(x: JBool, y: JBool): JBool = JBool(x.toBoolean && y.toBoolean)
  }
}

object JNum { self =>
  def apply(n: Long): JNum = LongNum(n)
  def apply(n: Double): JNum = DoubleNum(n)
  def apply(s: String): JNum = DeferNum(s)

  val zero: JNum = LongNum(0)
  val one: JNum = LongNum(1)

  implicit val algebra = new Field[JNum] with IsReal[JNum] with NRoot[JNum] with Order[JNum] {
    def zero: JNum = self.zero
    def one: JNum = self.one

    def abs(x: JNum): JNum = DoubleNum(x.toDouble)
    def compare(x: JNum, y: JNum): Int = x.toDouble compare y.toDouble
    def signum(x: JNum): Int = x.toDouble.signum

    def negate(x: JNum): JNum = DoubleNum(-x.toDouble)
    override def reciprocal(x: JNum): JNum = DoubleNum(1.0 / x.toDouble)
    def plus(x: JNum, y: JNum): JNum = DoubleNum(x.toDouble + y.toDouble)
    override def minus(x: JNum, y: JNum): JNum = DoubleNum(x.toDouble - y.toDouble)
    def times(x: JNum, y: JNum): JNum = DoubleNum(x.toDouble * y.toDouble)
    def div(x: JNum, y: JNum): JNum = DoubleNum(x.toDouble / y.toDouble)
    def mod(x: JNum, y: JNum): JNum = DoubleNum(x.toDouble % y.toDouble)
    def quot(x: JNum, y: JNum): JNum = DoubleNum(x.toDouble /~ y.toDouble)
    def gcd(x: JNum, y: JNum): JNum = DoubleNum(x.toDouble gcd y.toDouble)

    def ceil(x: JNum): JNum = DoubleNum(x.toDouble.ceil)
    def floor(x: JNum): JNum = DoubleNum(x.toDouble.floor)
    def round(x: JNum): JNum = DoubleNum(x.toDouble.round)
    def isWhole(x: JNum): Boolean = x.toDouble.isWhole
    def toDouble(x: JNum): Double = x.toDouble

    def fpow(x: JNum, y: JNum): JNum = DoubleNum(x.toDouble fpow y.toDouble)
    def nroot(x: JNum, k: Int): JNum = DoubleNum(x.toDouble nroot k)
  }
}

object JString {

  val empty = JString("")

  implicit val monoid = new Monoid[JString] {
    def id: JString = empty
    def op(x: JString, y: JString): JString = x + y
  }
}

object JArray { self =>

  val empty = JArray(new Array[JValue](0))

  implicit val monoid = new Monoid[JArray] {
    def id: JArray = self.empty
    def op(x: JArray, y: JArray): JArray = JArray(x.vs ++ y.vs)
  }

  def fromSeq(js: Seq[JValue]): JArray = JArray(js.toArray)
}

object JObject { self =>

  def empty = JObject(mutable.Map.empty)

  implicit val monoid = new Monoid[JObject] {
    def id: JObject = self.empty
    def op(x: JObject, y: JObject): JObject = JObject(x.vs ++ y.vs)
  }

  def fromSeq(js: Seq[(String, JValue)]): JObject = JObject(mutable.Map(js: _*))
}
