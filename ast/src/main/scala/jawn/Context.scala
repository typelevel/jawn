package jawn.ast

import scala.collection.mutable

private[jawn] sealed trait Context {
  def add(s: String): Unit
  def add(v: JValue): Unit
  def finish: JValue
  def isObj: Boolean
}

private[jawn] final class SingleContext extends Context {
  var value: JValue = null
  def add(s: String): Unit = value = JString(s)
  def add(v: JValue): Unit = value = v
  def finish = value
  def isObj = false
}

private[jawn] final class ArrContext extends Context {
  private val vs = mutable.ArrayBuffer.empty[JValue]

  def add(s: String): Unit = vs.append(JString(s))
  def add(v: JValue): Unit = vs.append(v)
  def finish = new JArray(vs.toArray)
  def isObj = false
}

private[jawn] final class ObjContext extends Context {
  private var key: String = null
  private val vs = mutable.Map.empty[String, JValue]

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
