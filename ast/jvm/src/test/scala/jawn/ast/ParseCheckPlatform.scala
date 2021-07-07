package org.typelevel.jawn
package ast

import org.scalacheck.Prop
import org.typelevel.jawn.parser.TestUtil

import ArbitraryUtil._
import Prop.forAll

private[jawn] trait AstCheckPlatform { self: AstCheck =>

  // Rendering/parsing numbers on JS isn't always idempotent

  // so it's only one property, but it exercises:
  //
  // * parsing from strings
  // * rendering jvalues to string
  // * jvalue equality
  //
  // not bad.
  property("idempotent parsing/rendering") = forAll { (value1: JValue) =>
    val json1 = CanonicalRenderer.render(value1)
    val value2 = JParser.parseFromString(json1).get
    val json2 = CanonicalRenderer.render(value2)

    val p0: Prop = Prop(
      json2 == json1 &&
        json2.## == json1.## &&
        value1 == value2 &&
        value1.## == value2.##
    )

    val p1: Prop = TestUtil.withTemp(json1)(t => Prop(JParser.parseFromFile(t).get == value2))

    p0 && p1
  }

  import AsyncParser.SingleValue

  property("async parsing") = forAll { (v: JValue) =>
    val json = CanonicalRenderer.render(v)
    val segments = splitIntoSegments(json)
    val parsed = parseSegments(AsyncParser[JValue](SingleValue), segments)
    Prop(parsed == List(v))
  }

  property("large strings") = {
    val M = 1000000
    val q = "\""

    val s0 = "x" * (40 * M)
    val e0 = q + s0 + q
    val p0: Prop = TestUtil.withTemp(e0)(t => Prop(JParser.parseFromFile(t).filter(_ == JString(s0)).isSuccess))

    val s1 = "\\" * (20 * M)
    val e1 = q + s1 + s1 + q
    val p1: Prop = TestUtil.withTemp(e1)(t => Prop(JParser.parseFromFile(t).filter(_ == JString(s1)).isSuccess))

    p0 && p1
  }

}
