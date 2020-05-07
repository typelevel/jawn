package org.typelevel.jawn
package support.json4s

import scala.collection.mutable
import org.json4s.JsonAST._

object Parser extends Parser(false, false)

class Parser(useBigDecimalForDouble: Boolean, useBigIntForLong: Boolean) extends SupportParser[JValue] {

  implicit val facade: Facade[JValue] =
    new Facade.NoIndexFacade[JValue] {
      def jnull: JValue = JNull
      val jfalse: JValue = JBool(false)
      val jtrue: JValue = JBool(true)

      def jnum(s: CharSequence, decIndex: Int, expIndex: Int): JValue =
        if (decIndex == -1 && expIndex == -1)
          if (useBigIntForLong) JInt(BigInt(s.toString))
          else JLong(util.parseLongUnsafe(s))
        else if (useBigDecimalForDouble) JDecimal(BigDecimal(s.toString))
        else JDouble(s.toString.toDouble)

      def jstring(s: CharSequence): JValue = JString(s.toString)

      def singleContext(): FContext[JValue] =
        new FContext.NoIndexFContext[JValue] {
          private[this] var value: JValue = null
          def add(s: CharSequence): Unit = value = jstring(s)
          def add(v: JValue): Unit = value = v
          def finish(): JValue = value
          def isObj: Boolean = false
        }

      def arrayContext(): FContext[JValue] =
        new FContext.NoIndexFContext[JValue] {
          private[this] val vs = mutable.ListBuffer.empty[JValue]
          def add(s: CharSequence): Unit = vs += jstring(s)
          def add(v: JValue): Unit = vs += v
          def finish(): JValue = JArray(vs.toList)
          def isObj: Boolean = false
        }

      def objectContext(): FContext[JValue] =
        new FContext.NoIndexFContext[JValue] {
          private[this] var key: String = null
          private[this] val vs = mutable.ListBuffer.empty[JField]
          def add(s: CharSequence): Unit =
            if (key == null) key = s.toString
            else {
              vs += JField(key, jstring(s)); key = null
            }
          def add(v: JValue): Unit = { vs += JField(key, v); key = null }
          def finish(): JValue = JObject(vs.toList)
          def isObj: Boolean = true
        }
    }
}
