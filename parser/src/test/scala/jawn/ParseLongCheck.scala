package jawn
package parser

import org.scalatest._
import prop._
import org.scalacheck._

import scala.util._

class ParseLongCheck extends PropSpec with Matchers with PropertyChecks {

  case class UniformLong(value: Long)

  object UniformLong {
    implicit val arbitraryUniformLong: Arbitrary[UniformLong] =
      Arbitrary(Gen.choose(Long.MinValue, Long.MaxValue).map(UniformLong(_)))
  }

  property("both parsers accept on valid input") {
    forAll { (n0: UniformLong, prefix: String, suffix: String) =>
      val n = n0.value
      val payload = n.toString
      val s = prefix + payload + suffix
      val i = prefix.length
      val cs = s.subSequence(i, payload.length + i)
      cs.toString shouldBe payload
      Util.parseLong(cs) shouldBe n
      Util.parseLongUnsafe(cs) shouldBe n
    }

    forAll { (s: String) =>
      Try(Util.parseLong(s)) match {
        case Success(n) => Util.parseLongUnsafe(s) shouldBe n
        case Failure(_) => succeed
      }
    }
  }

  property("safe parser fails on invalid input") {
    forAll { (n: Long, m: Long, suffix: String) =>
      val s1 = n.toString + suffix
      Try(Util.parseLong(s1)) match {
        case Success(n) => n shouldBe s1.toLong
        case Failure(_) => Try(s1.toLong).isFailure
      }

      val s2 = n.toString + (m & 0x7fffffffffffffffL).toString
      Try(Util.parseLong(s2)) match {
        case Success(n) => n shouldBe s2.toLong
        case Failure(_) => Try(s2.toLong).isFailure
      }
    }

    Try(Util.parseLong("9223372036854775807")) shouldBe Try(Long.MaxValue)
    Try(Util.parseLong("-9223372036854775808")) shouldBe Try(Long.MinValue)

    assert(Try(Util.parseLong("9223372036854775808")).isFailure)
    assert(Try(Util.parseLong("-9223372036854775809")).isFailure)
  }

  // NOTE: parseLongUnsafe is not guaranteed to crash, or do anything
  // predictable, on invalid input, so we don't test this direction.
  // Its "unsafe" suffix is there for a reason.
}
