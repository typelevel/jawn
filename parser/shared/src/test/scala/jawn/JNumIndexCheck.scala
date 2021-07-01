package org.typelevel.jawn
package parser

import java.nio.ByteBuffer
import org.scalacheck.Properties
import org.scalacheck.Prop
import org.scalacheck.Prop.forAll
import scala.util.Success

class JNumIndexCheck extends Properties("JNumIndexCheck") with JNumIndexCheckPlatform {

  object JNumIndexCheckFacade extends Facade.NoIndexFacade[Boolean] {
    class JNumIndexCheckContext(val isObj: Boolean) extends FContext.NoIndexFContext[Boolean] {
      var failed = false
      def add(s: CharSequence): Unit = ()
      def add(v: Boolean): Unit =
        if (!v) failed = true
      def finish(): Boolean = !failed
    }

    def singleContext(): FContext[Boolean] = new JNumIndexCheckContext(false)
    def arrayContext(): FContext[Boolean] = new JNumIndexCheckContext(false)
    def objectContext(): FContext[Boolean] = new JNumIndexCheckContext(true)

    def jnull: Boolean = true
    def jfalse: Boolean = true
    def jtrue: Boolean = true
    def jnum(s: CharSequence, decIndex: Int, expIndex: Int): Boolean = {
      val input = s.toString
      val inputDecIndex = input.indexOf('.')
      val inputExpIndex = if (input.indexOf('e') == -1) input.indexOf("E") else input.indexOf('e')

      decIndex == inputDecIndex && expIndex == inputExpIndex
    }
    def jstring(s: CharSequence): Boolean = true
  }

  property("jnum provides the correct indices with parseFromString") = forAll { (value: BigDecimal) =>
    val json = s"""{ "num": ${value.toString} }"""
    Prop(Parser.parseFromString(json)(JNumIndexCheckFacade) == Success(true))
  }

  property("jnum provides the correct indices with parseFromByteBuffer") = forAll { (value: BigDecimal) =>
    val json = s"""{ "num": ${value.toString} }"""
    val bb = ByteBuffer.wrap(json.getBytes("UTF-8"))
    Prop(Parser.parseFromByteBuffer(bb)(JNumIndexCheckFacade) == Success(true))
  }

  property("jnum provides the correct indices with parseFromByteArray") = forAll { (value: BigDecimal) =>
    val json = s"""{ "num": ${value.toString} }"""
    val ba = json.getBytes("UTF-8")
    Prop(Parser.parseFromByteArray(ba)(JNumIndexCheckFacade) == Success(true))
  }

  property("jnum provides the correct indices at the top level with parseFromString") = forAll { (value: BigDecimal) =>
    Prop(Parser.parseFromString(value.toString)(JNumIndexCheckFacade) == Success(true))
  }

  property("jnum provides the correct indices at the top level with parseFromByteBuffer") = forAll {
    (value: BigDecimal) =>
      val bb = ByteBuffer.wrap(value.toString.getBytes("UTF-8"))
      Prop(Parser.parseFromByteBuffer(bb)(JNumIndexCheckFacade) == Success(true))
  }

  property("jnum provides the correct indices at the top level with parseFromByteArray") = forAll {
    (value: BigDecimal) =>
      val ba = value.toString.getBytes("UTF-8")
      Prop(Parser.parseFromByteArray(ba)(JNumIndexCheckFacade) == Success(true))
  }
}
