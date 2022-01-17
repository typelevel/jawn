/*
 * Copyright (c) 2022 Typelevel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.typelevel.jawn
package util

import org.scalacheck.{Arbitrary, Gen, Prop, Properties}
import scala.util.{Failure, Success, Try}

import Arbitrary.arbitrary
import Prop.forAll

class SliceCheck extends Properties("SliceCheck") with SliceCheckPlatform {

  val genSlice: Gen[Slice] = {
    val g = arbitrary[String]
    def c(start: Int, end: Int): Gen[Int] =
      if (end <= start) Gen.const(start)
      else Gen.choose(start, end)
    Gen.oneOf(
      g.map(Slice(_)),
      for { s <- g; n = s.length; i <- c(0, n) } yield Slice(s, i, n),
      for { s <- g; n = s.length; j <- c(0, n) } yield Slice(s, 0, j),
      for { s <- g; n = s.length; i <- c(0, n); j <- c(i, n) } yield Slice(s, i, j)
    )
  }

  implicit val arbitrarySlice: Arbitrary[Slice] =
    Arbitrary(genSlice)

  def tryEqual[A](got0: => A, expected0: => A): Prop = {
    val got = Try(got0)
    val expected = Try(expected0)
    got match {
      case Success(_) => Prop(got == expected)
      case Failure(_) => Prop(expected.isFailure)
    }
  }

  property("slice.length >= 0") = forAll((cs: Slice) => Prop(cs.length >= 0))

  property("Slice(s) ~ Slice(s, 0, s.length)") = forAll { (s: String) =>
    tryEqual(Slice(s).toString, Slice(s, 0, s.length).toString)
  }

  property("Slice(s, i, j) => Slice.unsafe(s, i, j)") = forAll { (s: String, i: Int, j: Int) =>
    Try(Slice(s, i, j).toString) match {
      case Success(r) => Prop(r == Slice.unsafe(s, i, j).toString)
      case Failure(_) => Prop(true)
    }
  }

  property("x == x") = forAll((x: Slice) => Prop(x == x))

  property("(x == y) = (x.toString == y.toString)") = forAll { (x: Slice, y: Slice) =>
    Prop((x == y) == (x.toString == y.toString))
  }

  property("(x == y) -> (x.## == y.##)") = forAll { (x: Slice, y: Slice) =>
    if (x == y) Prop(x.## == y.##) else Prop(x.## != y.##)
  }

  property("x == Slice(x.toString)") = forAll((x: Slice) => Prop(Slice(x.toString) == x))

}
