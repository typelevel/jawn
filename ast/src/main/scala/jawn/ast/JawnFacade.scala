package jawn
package ast

import scala.collection.mutable

object JawnFacade extends Facade[JValue] {

  final val jnull = JNull
  final val jfalse = JFalse
  final val jtrue = JTrue
  final def jnum(s: String) = DeferNum(s)
  final def jint(s: String) = DeferLong(s)
  final def jstring(s: String) = JString(s)

  final def singleContext(): FContext[JValue] =
    new FContext[JValue] {
      var value: JValue = _
      def add(s: String) { value = JString(s) }
      def add(v: JValue) { value = v }
      def finish: JValue = value
      def isObj: Boolean = false
    }

  final def arrayContext(): FContext[JValue] =
    new FContext[JValue] {
      val vs = mutable.ArrayBuffer.empty[JValue]
      def add(s: String) { vs.append(JString(s)) }
      def add(v: JValue) { vs.append(v) }
      def finish: JValue = JArray(vs.toArray)
      def isObj: Boolean = false
    }

  final def objectContext(): FContext[JValue] =
    new FContext[JValue] {
      var key: String = null
      val vs = mutable.Map.empty[String, JValue]
      def add(s: String): Unit =
        if (key == null) { key = s } else { vs(key) = JString(s); key = null }
      def add(v: JValue): Unit =
        { vs(key) = v; key = null }
      def finish = JObject(vs)
      def isObj = true
    }
}
