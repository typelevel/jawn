package jawn

import scala.collection.mutable

trait FContext[J] {
  def add(s: String): Unit
  def add(v: J): Unit
  def finish: J
  def isObj: Boolean
}

trait Facade[J] {
  def singleContext: FContext[J]
  def arrayContext: FContext[J]
  def objectContext: FContext[J]

  def jnull: J
  def jfalse: J
  def jtrue: J
  def jnum(s: String): J
  def jstring(s: String): J
}

object Facade {
  val Jawn = new Facade[JValue] {

    def singleContext = new FContext[JValue] {
      var value: JValue = null
      def add(s: String) { value = JString(s) }
      def add(v: JValue) { value = v }
      def finish: JValue = value
      def isObj: Boolean = false
    }

    def arrayContext = new FContext[JValue] {
      val vs = mutable.ArrayBuffer.empty[JValue]
      def add(s: String) { vs.append(JString(s)) }
      def add(v: JValue) { vs.append(v) }
      def finish: JValue = new JArray(vs.toArray)
      def isObj: Boolean = false
    }

    def objectContext = new FContext[JValue] {
      var key: String = null
      val vs = mutable.Map.empty[String, JValue]
      def add(s: String): Unit = if (key == null) {
        key = s
      } else {
        vs(key) = JString(s)
        key = null
      }

      def add(v: JValue): Unit = { vs(key) = v; key = null }

      def finish = JObject(vs)
      def isObj = true
    }

    def jnull = JNull
    def jfalse = JFalse
    def jtrue = JTrue
    def jnum(s: String) = DeferNum(s)
    def jstring(s: String) = JString(s)
  }
}
