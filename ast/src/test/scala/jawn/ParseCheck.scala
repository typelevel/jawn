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

class AstCheck extends PropSpec with Matchers with GeneratorDrivenPropertyChecks {

  // in theory we could test larger number values than longs, but meh?
  // we need to exclude nan, +inf, and -inf from our doubles
  // we want to be sure we test every possible unicode character

  val jnull   = Gen.oneOf(JNull :: Nil)
  val jfalse  = Gen.oneOf(JFalse :: Nil)
  val jtrue   = Gen.oneOf(JTrue :: Nil)
  val jlong   = arbitrary[Long].map(LongNum(_))
  val jdouble = Gen.choose(Double.MinValue, Double.MaxValue).map(DoubleNum(_))
  val jstring = arbitrary[String].map(JString(_))

  // totally unscientific atom frequencies
  val jatom: Gen[JAtom] =
    Gen.frequency((1, 'n), (5, 'f), (5, 't), (8, 'l), (8, 'd), (16, 's)).flatMap {
      case 'n => jnull
      case 'f => jfalse
      case 't => jtrue
      case 'l => jlong
      case 'd => jdouble
      case 's => jstring
    }

  // use lvl to limit the depth of our jvalues
  // otherwise we will end up with SOE real fast

  def jarray(lvl: Int): Gen[JArray] =
    Gen.containerOf[Array, JValue](jvalue(lvl + 1)).map(JArray(_))

  def jitem(lvl: Int): Gen[(String, JValue)] =
    for { s <- arbitrary[String]; j <- jvalue(lvl) } yield (s, j)

  def jobject(lvl: Int): Gen[JObject] =
    Gen.containerOf[List, (String, JValue)](jitem(lvl + 1)).map(JObject.fromSeq)

  def jvalue(lvl: Int): Gen[JValue] =
    if (lvl < 3) {
      Gen.frequency((16, 'ato), (1, 'arr), (2, 'obj)).flatMap {
        case 'ato => jatom
        case 'arr => jarray(lvl)
        case 'obj => jobject(lvl)
      }
    } else {
      jatom
    }

  implicit lazy val arbJAtom: Arbitrary[JAtom] =
    Arbitrary(jatom)

  // implicit lazy val arbJArray: Arbitrary[JArray] =
  //   Arbitrary(jarray(3))
  
  implicit lazy val arbJValue: Arbitrary[JValue] =
    Arbitrary(jvalue(0))

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
      parseSegments(AsyncParser[JValue](SingleValue), segments) shouldBe List(v)
    }
  }

  property("async unwrapping") {
    //forAll { (vs: List[JAtom]) =>
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
      val n3 = DeferNum(s + ".00")

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
