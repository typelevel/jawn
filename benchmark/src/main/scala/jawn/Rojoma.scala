package jawn

import scala.collection.mutable

object Rojoma extends MutableFacade[com.rojoma.json.ast.JValue] {
  import com.rojoma.json.ast._
  def jnull() = JNull
  def jfalse() = JBoolean(false)
  def jtrue() = JBoolean(true)
  def jnum(s: String) = JNumber(BigDecimal(s))
  def jint(s: String) = JNumber(BigDecimal(s))
  def jstring(s: String) = JString(s)
  def jarray(vs: mutable.ArrayBuffer[JValue]) = JArray(vs)
  def jobject(vs: mutable.Map[String, JValue]) = JObject(vs)
}
