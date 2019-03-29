package org.typelevel.jawn
package support.argonaut

import argonaut.{Argonaut, CodecJson, Json}
import claimant.Claim
import org.scalacheck.{Arbitrary, Prop, Properties}
import scala.util.Try

import Arbitrary.arbitrary
import Argonaut._
import Prop.forAll

object ParserSpec {
  case class Example(a: Int, b: Long, c: Double)

  val exampleCodecJson: CodecJson[Example] =
    casecodec3(Example.apply, Example.unapply)("a", "b", "c")

  implicit val exampleCaseClassArbitrary: Arbitrary[Example] =
    Arbitrary(for {
      a <- arbitrary[Int]
      b <- arbitrary[Long]
      c <- arbitrary[Double]
    } yield Example(a, b, c))
}

class ParserSpec extends Properties("ParserSpec") {

  import ParserSpec._
  import org.typelevel.jawn.support.argonaut.Parser.facade

  property("argonaut support correctly marshals case class with Long values") =
    forAll { (e: Example) =>
      val jsonString: String = exampleCodecJson.encode(e).nospaces
      val json: Try[Json] = org.typelevel.jawn.Parser.parseFromString(jsonString)
      Claim(exampleCodecJson.decodeJson(json.get).toOption == Some(e))
    }
}
