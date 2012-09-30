package jawn

import debox.buffer._
import debox.map._

sealed trait Context {
  def add(s: String): Unit
  def add(v: Value): Unit
  def finish: Container
  def isObj: Boolean
}

final class ArrContext extends Context {
  private val vs = Mutable.empty[Value]

  def add(s: String): Unit = vs.append(Str(s))
  def add(v: Value): Unit = vs.append(v)
  def finish: Arr = new Arr(vs)
  def isObj = false
}

final class ObjContext extends Context {
  implicit val u = debox.Unset.Implicits.anyrefHasNullUnset[String]
  private var key: String = null
  private val vs = Map.empty[String, Value]

  def add(s: String): Unit = if (key == null) {
    key = s
  } else {
    vs(key) = Str(s)
    key = null
  }

  def add(v: Value): Unit = { vs(key) = v; key = null }

  def finish: Obj = new Obj(vs)
  def isObj = true
}
