package org.typelevel.jawn
package ast

import scala.collection.mutable

object JawnFacade extends Facade[JValue] {

  final def jnull(): JValue = JNull
  final def jfalse(): JValue = JFalse
  final def jtrue(): JValue = JTrue

  final def jnum(s: CharSequence, decIndex: Int, expIndex: Int): JValue =
    if (decIndex == -1 && expIndex == -1) {
      DeferLong(s.toString)
    } else {
      DeferNum(s.toString)
    }

  final def jstring(s: CharSequence): JValue =
    JString(s.toString)

  final def singleContext(): RawFContext[JValue] =
    new FContext[JValue] {
      var value: JValue = _
      def add(s: CharSequence): Unit = value = JString(s.toString)
      def add(v: JValue): Unit = value = v
      def finish(): JValue = value
      def isObj: Boolean = false
    }

  final def arrayContext(): RawFContext[JValue] =
    new FContext[JValue] {
      val vs = mutable.ArrayBuffer.empty[JValue]
      def add(s: CharSequence): Unit = vs += JString(s.toString)
      def add(v: JValue): Unit = vs += v
      def finish(): JValue = JArray(vs.toArray)
      def isObj: Boolean = false
    }

  final def objectContext(): RawFContext[JValue] =
    new FContext[JValue] {
      var key: String = null
      val vs = mutable.Map.empty[String, JValue]
      def add(s: CharSequence): Unit =
        if (key == null) {
          key = s.toString
        } else {
          vs(key.toString) = JString(s.toString); key = null
        }
      def add(v: JValue): Unit = { vs(key) = v; key = null }
      def finish() = JObject(vs)
      def isObj: Boolean = true
    }
}
