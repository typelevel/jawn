/*
 * Copyright (c) 2012 Typelevel
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

import Prop.forAll

class ParseLongCheck extends Properties("ParseLongCheck") {

  case class UniformLong(value: Long)

  object UniformLong {
    implicit val arbitraryUniformLong: Arbitrary[UniformLong] =
      Arbitrary(Gen.choose(Long.MinValue, Long.MaxValue).map(UniformLong(_)))
  }

  property("both parsers accept on valid input") = forAll { (n0: UniformLong, prefix: String, suffix: String) =>
    // new Exception("ok").printStackTrace()
    // sys.error("!")
    val n = n0.value
    val payload = n.toString
    val s = prefix + payload + suffix
    val i = prefix.length
    val cs = s.subSequence(i, payload.length + i)
    Prop(
      cs.toString == payload &&
        parseLong(cs) == n &&
        parseLongUnsafe(cs) == n
    )
  }

  property("parsers agree on random input") = forAll { (s: String) =>
    Try(parseLong(s)) match {
      case Success(n) => Prop(parseLongUnsafe(s) == n)
      case Failure(_) => Prop(true)
    }
  }

  property("safe parser fails on invalid input") = forAll { (n1: Long, m: Long, suffix: String) =>
    val s1 = n1.toString + suffix
    val t = Try(parseLong(s1))
    // .toLong is laxer than parseLong, so a parseLong failure does
    // not imply a .toLong failure.
    val p1 = Prop(t.isFailure || t == Try(s1.toLong))

    // avoid leading zeros, which .toLong is lax about
    val n2 = if (n1 == 0L) 1L else n1
    val s2 = n2.toString + (m & 0x7fffffffffffffffL).toString
    val tx = Try(parseLong(s2)).toOption
    val ty = Try(s2.toLong).toOption
    val p2 = Prop(tx == ty)

    p1 && p2
  }

  property("safe parser fails on test cases") = Prop(parseLong("9223372036854775807") == Long.MaxValue) &&
    Prop(parseLong("-9223372036854775808") == Long.MinValue) &&
    Prop(parseLong("-0") == 0L) &&
    Prop(Try(parseLong("")).isFailure) &&
    Prop(Try(parseLong("+0")).isFailure) &&
    Prop(Try(parseLong("00")).isFailure) &&
    Prop(Try(parseLong("01")).isFailure) &&
    Prop(Try(parseLong("+1")).isFailure) &&
    Prop(Try(parseLong("-")).isFailure) &&
    Prop(Try(parseLong("--1")).isFailure) &&
    Prop(Try(parseLong("9223372036854775808")).isFailure) &&
    Prop(Try(parseLong("-9223372036854775809")).isFailure)

  // NOTE: parseLongUnsafe is not guaranteed to crash, or do anything
  // predictable, on invalid input, so we don't test this direction.
  // Its "unsafe" suffix is there for a reason.
}
