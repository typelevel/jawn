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
package ast

import org.scalacheck.{Arbitrary, Gen}

import Arbitrary.arbitrary

object ArbitraryUtil {

  // JSON doesn't allow NaN, PositiveInfinity, or NegativeInfinity
  def isFinite(n: Double): Boolean =
    !java.lang.Double.isNaN(n) && !java.lang.Double.isInfinite(n)

  val jnull = Gen.const(JNull)
  val jboolean = Gen.oneOf(JTrue :: JFalse :: Nil)
  val jlong = arbitrary[Long].map(LongNum(_))
  val jdouble = arbitrary[Double].filter(isFinite).map(DoubleNum(_))
  val jstring = arbitrary[String].map(JString(_))

  // Totally unscientific atom frequencies.
  val jatom: Gen[JAtom] =
    Gen.frequency((1, jnull), (8, jboolean), (8, jlong), (8, jdouble), (16, jstring))

  // Use lvl to limit the depth of our jvalues.
  // Otherwise we will end up with SOE real fast.

  val MaxLevel: Int = 3

  def jarray(lvl: Int): Gen[JArray] =
    Gen.containerOf[Array, JValue](jvalue(lvl + 1)).map(JArray(_))

  def jitem(lvl: Int): Gen[(String, JValue)] =
    for { s <- arbitrary[String]; j <- jvalue(lvl) } yield (s, j)

  def jobject(lvl: Int): Gen[JObject] =
    Gen.containerOf[Vector, (String, JValue)](jitem(lvl + 1)).map(JObject.fromSeq)

  def jvalue(lvl: Int = 0): Gen[JValue] =
    if (lvl >= MaxLevel) jatom
    else Gen.frequency((16, jatom), (1, jarray(lvl)), (2, jobject(lvl)))

  implicit lazy val arbitraryJValue: Arbitrary[JValue] =
    Arbitrary(jvalue())

  // Valid JSON numbers with an exact double representation and in the Long range

  implicit lazy val expNotationNums: List[(String, Double)] = List(
    ("2e3", 2e3),
    ("2.5e0", 2.5e0),
    ("2e+3", 2e+3),
    ("2.5e-1", 2.5e-1),
    ("9.223372036854776e18", 9.223372036854776e18),
    ("-9.223372036854776e+18", -9.223372036854776e18)
  )
}
