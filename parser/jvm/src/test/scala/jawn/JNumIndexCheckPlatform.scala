package org.typelevel.jawn
package parser

import org.scalacheck.Prop
import org.scalacheck.Prop.forAll
import scala.util.Success

private[jawn] trait JNumIndexCheckPlatform { self: JNumIndexCheck =>

  property("jnum provides the correct indices with parseFromFile") = forAll { (value: BigDecimal) =>
    val json = s"""{ "num": ${value.toString} }"""
    TestUtil.withTemp(json)(t => Prop(Parser.parseFromFile(t)(JNumIndexCheckFacade) == Success(true)))
  }

  property("jnum provides the correct indices at the top level with parseFromFile") = forAll { (value: BigDecimal) =>
    TestUtil.withTemp(value.toString)(t => Prop(Parser.parseFromFile(t)(JNumIndexCheckFacade) == Success(true)))
  }

}
