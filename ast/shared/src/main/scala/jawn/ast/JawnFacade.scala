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

import scala.collection.mutable

object JawnFacade extends Facade.NoIndexFacade[JValue] {

  final def jnull: JValue = JNull
  final def jfalse: JValue = JFalse
  final def jtrue: JValue = JTrue

  final def jnum(s: CharSequence, decIndex: Int, expIndex: Int): JValue =
    if (decIndex == -1 && expIndex == -1)
      DeferLong(s.toString)
    else
      DeferNum(s.toString)

  final def jstring(s: CharSequence): JValue =
    JString(s.toString)

  final def singleContext(): FContext[JValue] =
    new FContext.NoIndexFContext[JValue] {
      private[this] var value: JValue = _
      def add(s: CharSequence): Unit = value = JString(s.toString)
      def add(v: JValue): Unit = value = v
      def finish(): JValue = value
      def isObj: Boolean = false
    }

  final def arrayContext(): FContext[JValue] =
    new FContext.NoIndexFContext[JValue] {
      private[this] val vs = mutable.ArrayBuffer.empty[JValue]
      def add(s: CharSequence): Unit = vs += JString(s.toString)
      def add(v: JValue): Unit = vs += v
      def finish(): JValue = JArray(vs.toArray)
      def isObj: Boolean = false
    }

  final def objectContext(): FContext[JValue] =
    new FContext.NoIndexFContext[JValue] {
      private[this] var key: String = null
      private[this] val vs = mutable.TreeMap.empty[String, JValue]
      def add(s: CharSequence): Unit =
        if (key == null)
          key = s.toString
        else {
          vs(key.toString) = JString(s.toString); key = null
        }
      def add(v: JValue): Unit = { vs(key) = v; key = null }
      def finish() = JObject(vs)
      def isObj: Boolean = true
    }
}
