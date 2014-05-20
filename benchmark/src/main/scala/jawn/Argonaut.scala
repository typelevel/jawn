package jawn

import argonaut._

object Argonaut extends Facade[Json] {

  def jnull() = Json.jNull
  def jfalse() = Json.jFalse
  def jtrue() = Json.jTrue
  def jnum(s: String) = Json.jNumberOrNull(java.lang.Double.parseDouble(s))
  def jint(s: String) = Json.jNumberOrNull(java.lang.Integer.parseInt(s))
  def jstring(s: String) = Json.jString(s)

  def singleContext() = new FContext[Json] {
    var value: Json = null
    def add(s: String) { value = jstring(s) }
    def add(v: Json) { value = v }
    def finish: Json = value
    def isObj: Boolean = false
  }

  def arrayContext() = new FContext[Json] {
    var vs = List.empty[Json]
    def add(s: String) { vs = jstring(s) :: vs }
    def add(v: Json) { vs = v :: vs }
    def finish: Json = Json.jArray(vs)
    def isObj: Boolean = false
  }

  def objectContext() = new FContext[Json] {
    var key: String = null
    var vs = JsonObject.empty
    def add(s: String): Unit =
      if (key == null) { key = s } else { vs = vs + (key, jstring(s)); key = null }
    def add(v: Json): Unit =
      { vs = vs + (key, v); key = null }
    def finish = Json.jObject(vs)
    def isObj = true
  }
}
