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

import org.scalacheck.{Prop, Properties}
import scala.util.Success

import ArbitraryUtil._
import Prop.forAll

class AstCheck extends Properties("AstCheck") with AstCheckPlatform {

  property("string encoding/decoding") = forAll { (s: String) =>
    val jstr1 = JString(s)
    val json1 = CanonicalRenderer.render(jstr1)
    val jstr2 = JParser.parseFromString(json1).get
    val json2 = CanonicalRenderer.render(jstr2)
    Prop(
      jstr2 == jstr1 &&
        json2 == json1 &&
        json2.## == json1.##
    )
  }

  implicit val facade: Facade[JValue] = JawnFacade

  val percs = List(0.0, 0.2, 0.4, 0.8, 1.0)

  def checkRight(r: Either[ParseException, collection.Seq[JValue]]): collection.Seq[JValue] =
    r match {
      case Right(vs) => vs
      case left @ Left(_) => sys.error(s"expected right got $left")
    }

  def splitIntoSegments(json: String): List[String] =
    if (json.length >= 8) {
      val offsets = percs.map(n => (json.length * n).toInt)
      val pairs = offsets.zip(offsets.drop(1))
      pairs.map { case (i, j) => json.substring(i, j) }
    } else
      json :: Nil

  def parseSegments(p: AsyncParser[JValue], segments: List[String]): collection.Seq[JValue] =
    segments.foldLeft(List.empty[JValue]) { (rs, s) =>
      rs ++ checkRight(p.absorb(s))
    } ++ checkRight(p.finish())

  import AsyncParser.{UnwrapArray, ValueStream}

  property("async multi") = {
    val data = "[1,2,3][4,5,6]"
    val p = AsyncParser[JValue](ValueStream)
    p.absorb(data)
    p.finish()
    Prop(true)
  }

  property("async unwrapping") = forAll { (vs0: List[Int]) =>
    val vs = vs0.map(v0 => LongNum(v0.toLong))
    val arr = JArray(vs.toArray)
    val json = CanonicalRenderer.render(arr)
    val segments = splitIntoSegments(json)
    Prop(parseSegments(AsyncParser[JValue](UnwrapArray), segments) == vs)
  }

  property("unicode string round-trip") = forAll { (s: String) =>
    Prop(JParser.parseFromString(JString(s).render(FastRenderer)) == Success(JString(s)))
  }

  property("if x == y, then x.## == y.##") = forAll { (x: JValue, y: JValue) =>
    if (x == y) Prop(x.## == y.##) else Prop(true)
  }

  property("ignore trailing zeros") = forAll { (n: Int) =>
    val s = n.toString
    val n1 = LongNum(n.toLong)
    val n2 = DoubleNum(n.toDouble)

    def check(j: JValue): Prop =
      Prop(j == n1 && n1 == j && j == n2 && n2 == j)

    check(DeferNum(s)) &&
    check(DeferNum(s + ".0")) &&
    check(DeferNum(s + ".00")) &&
    check(DeferNum(s + ".000")) &&
    check(DeferNum(s + "e0")) &&
    check(DeferNum(s + ".0e0"))
  }

}
