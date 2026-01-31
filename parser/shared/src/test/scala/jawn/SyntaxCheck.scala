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
package parser

import java.nio.ByteBuffer
import org.scalacheck.{Arbitrary, Gen, Prop, Properties}
import scala.util.{Failure, Success, Try}

import Prop.forAll
import Facade.NullFacade

class SyntaxCheck extends Properties("SyntaxCheck") with SyntaxCheckPlatform {

  sealed trait J {
    def build: String =
      this match {
        case JAtom(s) => s
        case JArray(js) => js.map(_.build).mkString("[", ",", "]")
        case JObject(js) =>
          js.map { case (k, v) =>
            val kk = "\"" + k + "\""
            val vv = v.build
            s"$kk: $vv"
          }.mkString("{", ",", "}")
      }
  }

  case class JAtom(s: String) extends J
  case class JArray(js: List[J]) extends J
  case class JObject(js: Map[String, J]) extends J

  val jatom: Gen[JAtom] =
    Gen
      .oneOf(
        "null",
        "true",
        "false",
        "1234",
        "-99",
        "16.0",
        "2e9",
        "-4.44E-10",
        "11e+14",
        "\"foo\"",
        "\"\"",
        "\"bar\"",
        "\"qux\"",
        "\"duh\"",
        "\"abc\"",
        "\"xyz\"",
        "\"zzzzzz\"",
        "\"\\u1234\""
      )
      .map(JAtom(_))

  def jarray(lvl: Int): Gen[JArray] =
    Gen.containerOf[List, J](jvalue(lvl + 1)).map(JArray(_))

  val keys = Gen.oneOf("foo", "bar", "qux", "abc", "def", "xyz")
  def jitem(lvl: Int): Gen[(String, J)] =
    for { s <- keys; j <- jvalue(lvl) } yield (s, j)

  def jobject(lvl: Int): Gen[JObject] =
    Gen.containerOf[List, (String, J)](jitem(lvl + 1)).map(ts => JObject(ts.toMap))

  def jvalue(lvl: Int): Gen[J] =
    if (lvl < 3)
      Gen.frequency((16, Symbol("ato")), (1, Symbol("arr")), (2, Symbol("obj"))).flatMap {
        case Symbol("ato") => jatom
        case Symbol("arr") => jarray(lvl)
        case Symbol("obj") => jobject(lvl)
      }
    else
      jatom

  implicit lazy val arbJValue: Arbitrary[J] =
    Arbitrary(jvalue(0))

  def isValidSyntax(s: String): Boolean = {
    val cs = java.nio.CharBuffer.wrap(s.toCharArray)
    val r0 = Parser.parseFromCharSequence(cs)(NullFacade).isSuccess
    val r1 = Parser.parseFromString(s)(NullFacade).isSuccess
    val bb = ByteBuffer.wrap(s.getBytes("UTF-8"))
    val r2 = Parser.parseFromByteBuffer(bb)(NullFacade).isSuccess
    if (r0 == r1) r1 else sys.error(s"CharSequence/String parsing disagree($r0, $r1): $s")
    if (r1 == r2) r1 else sys.error(s"String/ByteBuffer parsing disagree($r1, $r2): $s")

    isValidSyntaxPlatform(s)

    val async = AsyncParser[Unit](AsyncParser.SingleValue)
    val r3 = async.finalAbsorb(s)(NullFacade) match {
      case Right(xs) => xs.size == 1
      case Left(_) => false
    }

    if (r1 == r3) r1 else sys.error(s"Sync/Async parsing disagree($r1, $r3): $s")

    val r4 = Parser.parseFromByteArray(s.getBytes("UTF-8"))(NullFacade).isSuccess
    if (r1 == r4) r1 else sys.error(s"String/ByteArray parsing disagree($r1, $r4): $s")
  }

  property("syntax-checking") = forAll((j: J) => isValidSyntax(j.build))

  def qs(s: String): String = "\"" + s + "\""

  property("unicode is ok") = {
    isValidSyntax(qs("ö"))
    isValidSyntax(qs("ö\\\\"))
    isValidSyntax(qs("\\\\ö"))
  }

  property("valid unicode is ok") = isValidSyntax("\"\\u0000\"") &&
    isValidSyntax("\"\\uffff\"") &&
    isValidSyntax("\"\\uFFFF\"")

  property("invalid unicode is invalid") = isValidSyntax("\"\\uqqqq\"") != true &&
    isValidSyntax("\"\\ugggg\"") != true &&
    isValidSyntax("\"\\uGGGG\"") != true &&
    isValidSyntax("\"\\ughij\"") != true &&
    isValidSyntax("\"\\uklmn\"") != true &&
    isValidSyntax("\"\\uopqr\"") != true &&
    isValidSyntax("\"\\ustuv\"") != true &&
    isValidSyntax("\"\\uwxyz\"") != true &&
    isValidSyntax("\"\\u1\"") != true &&
    isValidSyntax("\"\\u12\"") != true &&
    isValidSyntax("\"\\u123\"") != true

  property("empty is invalid") = isValidSyntax("") != true
  property("} is invalid") = isValidSyntax("}") != true

  property("literal TAB is invalid") = isValidSyntax(qs("\t")) != true
  property("literal NL is invalid") = isValidSyntax(qs("\n")) != true
  property("literal CR is invalid") = isValidSyntax(qs("\r")) != true
  property("literal NUL is invalid") = isValidSyntax(qs("\u0000")) != true
  property("literal BS TAB is invalid") = isValidSyntax(qs("\\\t")) != true
  property("literal BS NL is invalid") = isValidSyntax(qs("\\\n")) != true
  property("literal BS CR is invalid") = isValidSyntax(qs("\\\r")) != true
  property("literal BS NUL is invalid") = isValidSyntax(qs("\\\u0000")) != true
  property("literal BS ZERO is invalid") = isValidSyntax(qs("\\0")) != true
  property("literal BS X is invalid") = isValidSyntax(qs("\\x")) != true

  property("0 is ok") = isValidSyntax("0")
  property("0e is invalid") = isValidSyntax("0e") != true
  property("123e is invalid") = isValidSyntax("123e") != true
  property(".999 is invalid") = isValidSyntax(".999") != true
  property("0.999 is ok") = isValidSyntax("0.999")
  property("-.999 is invalid") = isValidSyntax("-.999") != true
  property("-0.999 is ok") = isValidSyntax("-0.999")
  property("+0.999 is invalid") = isValidSyntax("+0.999") != true
  property("--0.999 is invalid") = isValidSyntax("--0.999") != true
  property("01 is invalid") = isValidSyntax("01") != true
  property("1e is invalid") = isValidSyntax("1e") != true
  property("1e- is invalid") = isValidSyntax("1e+") != true
  property("1e+ is invalid") = isValidSyntax("1e-") != true
  property("1. is invalid") = isValidSyntax("1.") != true
  property("1.e is invalid") = isValidSyntax("1.e") != true
  property("1.e9 is invalid") = isValidSyntax("1.e9") != true
  property("1.e- is invalid") = isValidSyntax("1.e+") != true
  property("1.e+ is invalid") = isValidSyntax("1.e-") != true
  property("1.1e is invalid") = isValidSyntax("1.1e") != true
  property("1.1e- is invalid") = isValidSyntax("1.1e-") != true
  property("1.1e+ is invalid") = isValidSyntax("1.1e+") != true
  property("1.1e1 is ok") = isValidSyntax("1.1e1")
  property("1.1e-1 is ok") = isValidSyntax("1.1e-1")
  property("1.1e+1 is ok") = isValidSyntax("1.1e+1")
  property("1+ is invalid") = isValidSyntax("1+") != true
  property("1- is invalid") = isValidSyntax("1-") != true

  def isStackSafe(s: String): Try[Boolean] =
    try Success(isValidSyntax(s))
    catch {
      case e: StackOverflowError => Failure(e)
      case e: Exception => Failure(e)
    }

  val S = "     " * 2000

  property("stack-safety 1") = Prop(isStackSafe(s"${S}[${S}null${S}]${S}") == Success(true))

  property("stack-safety 2") = Prop(isStackSafe(s"${S}[${S}nul${S}]${S}") == Success(false))

  property("stack-safety 3") = Prop(isStackSafe(S) == Success(false))

  property("stack-safety 4") = Prop(isStackSafe(s"${S}false${S}") == Success(true))

  property("stack-safety 5") = Prop(isStackSafe(s"${S}fals\\u0065${S}") == Success(false))

  property("stack-safety 6") = Prop(isStackSafe(s"${S}fals${S}") == Success(false))

  property("stack-safety 7") = Prop(isStackSafe(s"false${S}false") == Success(false))

  property("stack-safety 8") = Prop(isStackSafe(s"false${S},${S}false") == Success(false))

  def testErrorLoc(json: String, line: Int, col: Int): Prop = {

    def bb(s: String): ByteBuffer =
      ByteBuffer.wrap(s.getBytes("UTF-8"))

    def assertLoc(p: ParseException): Prop =
      Prop(p.line == line && p.col == col)

    def fail(msg: String): Prop =
      Prop.falsified :| msg

    def extract1(t: Try[Unit]): Prop =
      t match {
        case Failure(p @ ParseException(_, _, _, _)) => assertLoc(p)
        case otherwise => fail(s"expected Failure(ParseException), got $otherwise")
      }

    def extract2(e: Either[ParseException, collection.Seq[Unit]]): Prop =
      e match {
        case Left(p) => assertLoc(p)
        case right => fail(s"expected Left(ParseException), got $right")
      }

    Prop(isValidSyntax(json) != true) &&
    extract1(Parser.parseFromString(json)(NullFacade)) &&
    extract1(Parser.parseFromCharSequence(json)(NullFacade)) &&
    testErrorLocPlatform(json, line, col) &&
    extract1(Parser.parseFromByteBuffer(bb(json))(NullFacade)) &&
    extract2(Parser.async(AsyncParser.UnwrapArray).finalAbsorb(json)(NullFacade))
  }

  property("error location 1") = testErrorLoc("[1, 2,\nx3]", 2, 1)
  property("error location 2") = testErrorLoc("[1, 2,    \n   x3]", 2, 4)
  property("error location 3") = testErrorLoc("[1, 2,\n\n\n\n\nx3]", 6, 1)
  property("error location 4") = testErrorLoc("[1, 2,\n\n3,\n4,\n\n x3]", 6, 2)

  property("no extra \" in error message") = {
    val result = Parser.parseFromString("\"\u0000\"")(NullFacade)
    val expected = "control char (0) in string got '\u0000...' (line 1, column 2)"
    Prop(result.failed.get.getMessage == expected)
  }

  property("absorb should fail fast on bad inputs") = {

    def absorbFails(in: String): Boolean = {
      val async = AsyncParser[Unit](AsyncParser.UnwrapArray)
      async.absorb("}")(NullFacade).isLeft
    }

    val badInputs = Seq("}", "fälse", "n0ll", "try", "0x", "0.x", "0ex", "[1; 2]", "{\"a\"; 1}", "{1: 2}")

    Prop(badInputs.forall(absorbFails))
  }

  case class MultipleJsonArrays(jsonArrays: List[JArray]) {
    def build: String = jsonArrays.map(_.build).mkString
  }

  implicit val multipleJsonArraysArb: Arbitrary[MultipleJsonArrays] = Arbitrary {
    for {
      range <- Gen.choose(1, 5)
      arrays <- Gen.listOfN(range, jarray(0))
    } yield MultipleJsonArrays(arrays)
  }

  property("AsyncParser supports multiple top level JSON arrays in UnwrapMultiArray mode") = forAll {
    (multipleJsonArrays: MultipleJsonArrays) =>
      val result =
        AsyncParser[Unit](AsyncParser.UnwrapArray, multiValue = true).absorb(multipleJsonArrays.build)(NullFacade)
      result.isRight
  }

}
