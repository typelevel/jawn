package jawn
package ast

import scala.collection.mutable

object JawnFacade extends MutableFacade[JValue] {
  def jnull() = JNull
  def jfalse() = JFalse
  def jtrue() = JTrue
  def jnum(s: String) = DeferNum(s)
  def jint(s: String) = DeferNum(s)
  def jstring(s: String) = JString(s)
  def jarray(vs: mutable.ArrayBuffer[JValue]) = new JArray(vs.toArray)
  def jobject(vs: mutable.Map[String, JValue]) = JObject(vs)
}
