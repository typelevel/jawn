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
