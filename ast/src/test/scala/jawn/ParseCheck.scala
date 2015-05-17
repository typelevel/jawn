package jawn
package ast

import org.scalatest.matchers.ShouldMatchers
import org.scalatest._
import prop._
import org.scalacheck.Arbitrary._
import org.scalacheck._
import Gen._
import Arbitrary.arbitrary

import scala.collection.mutable
import scala.util.{Try, Success}

import ArbitraryUtil._

class AstCheck extends PropSpec with Matchers with GeneratorDrivenPropertyChecks {

  // so it's only one property, but it exercises:
  //
  // * parsing from strings
  // * rendering jvalues to string
  // * jvalue equality
  //
  // not bad.
  property("idempotent parsing/rendering") {
    forAll { value1: JValue =>
      val json1 = CanonicalRenderer.render(value1)
      val value2 = JParser.parseFromString(json1).get
      val json2 = CanonicalRenderer.render(value2)
      json2 shouldBe json1
      json2.## shouldBe json1.##

      value1 shouldBe value2
      value1.## shouldBe value2.##
    }
  }

  property("string encoding/decoding") {
    forAll { s: String =>
      val jstr1 = JString(s)
      val json1 = CanonicalRenderer.render(jstr1)
      val jstr2 = JParser.parseFromString(json1).get
      val json2 = CanonicalRenderer.render(jstr2)
      jstr2 shouldBe jstr1
      json2 shouldBe json1
      json2.## shouldBe json1.##
    }
  }

  implicit val facade = JawnFacade

  val percs = List(0.0, 0.2, 0.4, 0.8, 1.0)

  def checkRight(r: Either[ParseException, Seq[JValue]]): Seq[JValue] = {
    r.isRight shouldBe true
    val Right(vs) = r
    vs
  }

  def splitIntoSegments(json: String): List[String] = 
    if (json.length >= 8) {
      val offsets = percs.map(n => (json.length * n).toInt)
      val pairs = offsets zip offsets.drop(1)
      pairs.map { case (i, j) => json.substring(i, j) }
    } else {
      json :: Nil
    }

  def parseSegments(p: AsyncParser[JValue], segments: List[String]): Seq[JValue] =
    segments.foldLeft(List.empty[JValue]) { (rs, s) =>
      rs ++ checkRight(p.absorb(s))
    } ++ checkRight(p.finish())

  import AsyncParser.{UnwrapArray, ValueStream, SingleValue}

  property("async parsing") {
    forAll { (v: JValue) =>
      val json = CanonicalRenderer.render(v)
      val segments = splitIntoSegments(json)
      val parsed = parseSegments(AsyncParser[JValue](SingleValue), segments)
      parsed shouldBe List(v)
    }
  }

  property("async unwrapping") {
    forAll { (vs0: List[Int]) =>
      val vs = vs0.map(LongNum(_))
      val arr = JArray(vs.toArray)
      val json = CanonicalRenderer.render(arr)
      val segments = splitIntoSegments(json)
      parseSegments(AsyncParser[JValue](UnwrapArray), segments) shouldBe vs
    }
  }

  property("unicode string round-trip") {
    forAll { (s: String) =>
      JParser.parseFromString(JString(s).render(FastRenderer)) shouldBe Success(JString(s))
    }
  }

  property("if x == y, then x.## == y.##") {
    forAll { (x: JValue, y: JValue) =>
      if (x == y) {
        x.## shouldBe y.##
      }
    }
  }

  property("ignore trailing zeros") {
    forAll { (n: Int) =>
      val s = n.toString
      val n1 = LongNum(n)
      val n2 = DoubleNum(n)

      def check(j: JValue) {
        j shouldBe n1; n1 shouldBe j
        j shouldBe n2; n2 shouldBe j
      }

      check(DeferNum(s))
      check(DeferNum(s + ".0"))
      check(DeferNum(s + ".00"))
      check(DeferNum(s + ".000"))
      check(DeferNum(s + "e0"))
      check(DeferNum(s + ".0e0"))
    }
  }
}
