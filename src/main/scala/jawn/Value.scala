package jawn

import debox.buffer._
import debox.map._

sealed trait Value { def j: String }

case object True extends Value { def j = "true" }
case object False extends Value { def j = "false" }
case object Null extends Value { def j = "null" }

case class Str(s: String) extends Value { def j = "\"%s\"" format s } //fix
case class LongNum(n: Long) extends Value { def j = n.toString }
case class DoubleNum(n: Double) extends Value { def j = n.toString } //fix?

sealed trait Container extends Value

case class Arr(vs: Buffer[Value]) extends Container {
  def j = if (vs.length == 0) {
    "[]"
  } else {
    val sb = new StringBuilder().append("[")
    vs.foreach(v => sb.append(v.j).append(", "))
    sb.dropRight(2).append("]").toString
  }
}

case class Obj(vs: Map[String, Value]) extends Container {
  def j = if (vs.length == 0) {
    "{}"
  } else {
    val sb = new StringBuilder().append("[")
    vs.foreach {
      (k, v) => sb.append("\"" + k + "\": ").append(v.j).append(", ")
    }
    sb.dropRight(2).append("}").toString
  }
}
