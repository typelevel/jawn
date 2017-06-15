package jawn
package support.json4s

import scala.collection.mutable
import org.json4s.JsonAST._

object Parser extends Parser(false, false)

class Parser(useBigDecimalForDouble: Boolean, useBigIntForLong: Boolean) extends SupportParser[JValue] {

  implicit val facade: Facade[JValue] =
    new Facade[JValue] {
      def jnull() = JNull
      def jfalse() = JBool(false)
      def jtrue() = JBool(true)
      def jnum(s: String, decIndex: Int, expIndex: Int) =
        if (decIndex == -1 && expIndex == -1) {
          if (useBigIntForLong) JInt(BigInt(s))
          else JLong(s.toLong)
        } else {
          if (useBigDecimalForDouble) JDecimal(BigDecimal(s))
          else JDouble(s.toDouble)
        }

      def jstring(s: String) = JString(s)

      def singleContext() =
        new FContext[JValue] {
          var value: JValue = null
          def add(s: String) { value = jstring(s) }
          def add(v: JValue) { value = v }
          def finish: JValue = value
          def isObj: Boolean = false
        }

      def arrayContext() =
        new FContext[JValue] {
          val vs = mutable.ListBuffer.empty[JValue]
          def add(s: String) { vs += jstring(s) }
          def add(v: JValue) { vs += v }
          def finish: JValue = JArray(vs.toList)
          def isObj: Boolean = false
        }

      def objectContext() =
        new FContext[JValue] {
          var key: String = null
          val vs = mutable.ListBuffer.empty[JField]
          def add(s: String): Unit =
            if (key == null) key = s
            else { vs += JField(key, jstring(s)); key = null }
          def add(v: JValue): Unit =
            { vs += JField(key, v); key = null }
          def finish: JValue = JObject(vs.toList)
          def isObj: Boolean = true
        }
    }
}
