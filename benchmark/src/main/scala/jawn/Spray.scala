package jawn

import spray.json._

object Spray extends SimpleFacade[JsValue] {
  def jnull() = JsNull
  def jfalse() = JsFalse
  def jtrue() = JsTrue
  def jnum(s: String) = JsNumber(s)
  def jint(s: String) = JsNumber(s)
  def jstring(s: String) = JsString(s)
  def jarray(vs: List[JsValue]) = JsArray(vs)
  def jobject(vs: Map[String, JsValue]) = JsObject(vs)
}
