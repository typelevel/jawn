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

import scala.collection.mutable
import scala.collection.immutable.TreeMap
import scala.collection.JavaConverters._
import java.util.HashMap

/**
 * [[Facade]] is a type class that describes how Jawn should construct JSON AST elements of type `J`.
 *
 * `Facade[J]` also uses `FContext[J]` instances, so implementors will usually want to define both.
 */
trait Facade[J] {
  def singleContext(index: Int): FContext[J]
  def arrayContext(index: Int): FContext[J]
  def objectContext(index: Int): FContext[J]

  def jnull(index: Int): J
  def jfalse(index: Int): J
  def jtrue(index: Int): J
  def jnum(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): J
  def jstring(s: CharSequence, index: Int): J
  def jstring(s: CharSequence, start: Int, limit: Int): J = jstring(s, start)
}

object Facade {

  /**
   * A convenience trait for [[Facade]] implementers who don't need character offsets.
   */
  trait NoIndexFacade[J] extends Facade[J] {
    def singleContext(): FContext[J]
    def arrayContext(): FContext[J]
    def objectContext(): FContext[J]

    def jnull: J
    def jfalse: J
    def jtrue: J
    def jnum(s: CharSequence, decIndex: Int, expIndex: Int): J
    def jstring(s: CharSequence): J

    final def singleContext(index: Int): FContext[J] = singleContext()
    final def arrayContext(index: Int): FContext[J] = arrayContext()
    final def objectContext(index: Int): FContext[J] = objectContext()

    final def jnull(index: Int): J = jnull
    final def jfalse(index: Int): J = jfalse
    final def jtrue(index: Int): J = jtrue
    final def jnum(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): J =
      jnum(s, decIndex, expIndex)
    final def jstring(s: CharSequence, index: Int): J = jstring(s)
    final override def jstring(s: CharSequence, start: Int, limit: Int): J = jstring(s)
  }

  /**
   * A convenience trait for [[Facade]] implementers that doesn't require [[FContext]] implementations.
   */
  trait SimpleFacade[J] extends NoIndexFacade[J] {
    def jarray(vs: List[J]): J
    def jobject(vs: Map[String, J]): J

    final def singleContext(): FContext[J] =
      new FContext.NoIndexFContext[J] {
        private[this] var value: J = _
        def add(s: CharSequence): Unit = value = jstring(s)
        def add(v: J): Unit = value = v
        def finish(): J = value
        def isObj: Boolean = false
      }

    final def arrayContext(): FContext[J] =
      new FContext.NoIndexFContext[J] {
        private[this] val vs = mutable.ListBuffer.empty[J]
        def add(s: CharSequence): Unit = vs += jstring(s)
        def add(v: J): Unit = vs += v
        def finish(): J = jarray(vs.toList)
        def isObj: Boolean = false
      }

    final def objectContext(): FContext[J] =
      new FContext.NoIndexFContext[J] {
        private[this] var key: String = null
        private[this] var vs = TreeMap.empty[String, J]
        def add(s: CharSequence): Unit =
          if (key == null)
            key = s.toString
          else {
            vs = vs.updated(key, jstring(s)); key = null
          }
        def add(v: J): Unit = { vs = vs.updated(key, v); key = null }
        def finish(): J = jobject(vs)
        def isObj: Boolean = true
      }
  }

  /**
   * A convenience trait for [[Facade]] implementers that doesn't require [[FContext]] implementations and uses mutable
   * collections.
   */
  trait MutableFacade[J] extends NoIndexFacade[J] {
    def jarray(vs: mutable.ArrayBuffer[J]): J
    def jobject(vs: mutable.Map[String, J]): J

    final def singleContext(): FContext[J] =
      new FContext.NoIndexFContext[J] {
        private[this] var value: J = _
        def add(s: CharSequence): Unit = value = jstring(s)
        def add(v: J): Unit = value = v
        def finish(): J = value
        def isObj: Boolean = false
      }

    final def arrayContext(): FContext[J] =
      new FContext.NoIndexFContext[J] {
        private[this] val vs = mutable.ArrayBuffer.empty[J]
        def add(s: CharSequence): Unit = vs += jstring(s)
        def add(v: J): Unit = vs += v
        def finish(): J = jarray(vs)
        def isObj: Boolean = false
      }

    final def objectContext(): FContext[J] =
      new FContext.NoIndexFContext[J] {
        private[this] var key: String = null
        private[this] val vs = (new HashMap[String, J]).asScala
        def add(s: CharSequence): Unit =
          if (key == null)
            key = s.toString
          else {
            vs(key) = jstring(s); key = null
          }
        def add(v: J): Unit = { vs(key) = v; key = null }
        def finish(): J = jobject(vs)
        def isObj: Boolean = true
      }
  }

  /**
   * [[NullFacade]] discards all JSON AST information.
   *
   * This is the simplest possible facade. It could be useful for checking JSON for correctness (via parsing) without
   * worrying about saving the data.
   *
   * It will always return `()` on any successful parse, no matter the content.
   */
  object NullFacade extends NoIndexFacade[Unit] {
    private[this] val nullContext: FContext[Unit] = new FContext.NoIndexFContext[Unit] {
      def add(s: CharSequence): Unit = ()
      def add(v: Unit): Unit = ()
      def finish(): Unit = ()
      def isObj: Boolean = false
    }

    private[this] val nullObjectContext: FContext[Unit] = new FContext.NoIndexFContext[Unit] {
      def add(s: CharSequence): Unit = ()
      def add(v: Unit): Unit = ()
      def finish(): Unit = ()
      def isObj: Boolean = true
    }

    def singleContext(): FContext[Unit] = nullContext
    def arrayContext(): FContext[Unit] = nullContext
    def objectContext(): FContext[Unit] = nullObjectContext

    def jnull: Unit = ()
    def jfalse: Unit = ()
    def jtrue: Unit = ()
    def jnum(s: CharSequence, decIndex: Int, expIndex: Int): Unit = ()
    def jstring(s: CharSequence): Unit = ()
  }
}
