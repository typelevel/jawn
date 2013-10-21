package jawn

// import debox.buffer._
// import debox.map._
import scala.collection.mutable

sealed trait JValue { def j: String }

case object JTrue extends JValue { final def j = "true" }
case object JFalse extends JValue { final def j = "false" }
case object JNull extends JValue { final def j = "null" }

case class JString(s: String) extends JValue {
  def j = "\"%s\"" format Str.escape(s)
}

object Str {
  def escape(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
}

case class LongNum(n: Long) extends JValue { def j = n.toString }
case class DoubleNum(n: Double) extends JValue { def j = n.toString }
case class DeferNum(s: String) extends JValue { def j = s } //lol

object JNum {
  def apply(s: String): JValue = DeferNum(s)
}

sealed trait Container extends JValue

case class JArray(vs: Array[JValue]) extends Container {
  def j = if (vs.length == 0) {
    "[]"
  } else {
    val sb = new StringBuilder().append("[")
    vs.foreach(v => sb.append(v.j).append(", "))
    sb.dropRight(2).append("]").toString
  }
}

case class JObject(vs: mutable.Map[String, JValue]) extends Container {
  def j = if (vs.size == 0) {
    "{}"
  } else {
    val sb = new StringBuilder().append("[")
    vs.foreach { case (k, v) =>
      sb.append("\"" + k + "\": ").append(v.j).append(", ")
    }
    sb.dropRight(2).append("}").toString
  }
}
