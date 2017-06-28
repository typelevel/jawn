package jawn
package parser

import org.scalatest._
import prop._
import org.scalacheck._

import Arbitrary.arbitrary

import scala.util._

class SliceCheck extends PropSpec with Matchers with PropertyChecks {

  val genSlice: Gen[Slice] = {
    val g = arbitrary[String]
    def c(start: Int, end: Int): Gen[Int] =
      if (end <= start) Gen.const(start)
      else Gen.choose(start, end)
    Gen.oneOf(
      g.map(Slice(_)),
      for { s <- g; n = s.length; i <- c(0, n) } yield Slice(s, i, n),
      for { s <- g; n = s.length; j <- c(0, n) } yield Slice(s, 0, j),
      for { s <- g; n = s.length; i <- c(0, n); j <- c(i, n) } yield Slice(s, i, j))
  }

  implicit val arbitrarySlice: Arbitrary[Slice] =
    Arbitrary(genSlice)

  def tryEqual[A](got0: => A, expected0: => A): Unit = {
    val got = Try(got0)
    val expected = Try(expected0)
    got match {
      case Success(_) => got shouldBe expected
      case Failure(_) => assert(expected.isFailure)
    }
  }

  property("Slice(s, i, j) ~ s.substring(i, j)") {
    forAll { (s: String, i: Int, j: Int) =>
      tryEqual(
        Slice(s, i, j).toString,
        s.substring(i, j))
    }
  }

  property("Slice(s, i, j).charAt(k) ~ s.substring(i, j).charAt(k)") {
    forAll { (s: String, i: Int, j: Int, k: Int) =>
      tryEqual(
        Slice(s, i, j).charAt(k),
        s.substring(i, j).charAt(k))
    }
  }

  property("slice.length >= 0") {
    forAll { (cs: Slice) =>
      cs.length should be >= 0
    }
  }

  property("slice.charAt(i) ~ slice.toString.charAt(i)") {
    forAll { (cs: Slice, i: Int) =>
      tryEqual(
        cs.charAt(i),
        cs.toString.charAt(i))
    }
  }

  property("Slice(s, i, j).subSequence(k, l) ~ s.substring(i, j).substring(k, l)") {
    forAll { (s: String, i: Int, j: Int, k: Int, l: Int) =>
      tryEqual(
        Slice(s, i, j).subSequence(k, l).toString,
        s.substring(i, j).substring(k, l))
    }
  }

  property("Slice(s) ~ Slice(s, 0, s.length)") {
    forAll { (s: String) =>
      tryEqual(
        Slice(s).toString,
        Slice(s, 0, s.length).toString)
    }
  }

  property("Slice(s, i, j) => Slice.unsafe(s, i, j)") {
    forAll { (s: String, i: Int, j: Int) =>
      Try(Slice(s, i, j).toString) match {
        case Success(r) => r shouldBe Slice.unsafe(s, i, j).toString
        case Failure(_) => succeed
      }
    }
  }
}
