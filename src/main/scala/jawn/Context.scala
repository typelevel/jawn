package jawn

import debox.buffer._
import debox.map._

sealed trait Context {
  def add(v: Value): Unit
  def finish: Container
  def isObj: Boolean
}

final class ArrContext extends Context {
  private val vs = Mutable.empty[Value]

  def add(v: Value): Unit = vs.append(v)
  def finish: Arr = new Arr(vs)
  def isObj = false
}

final class ObjContext extends Context {
  implicit val u = debox.Unset.Implicits.anyrefHasNullUnset[String]
  private var key: String = null
  private val vs = Map.empty[String, Value]

  def addKey(k: String): Unit = key = k
  def add(v: Value): Unit = vs(key) = v
  def finish: Obj = new Obj(vs)
  def isObj = true
}
