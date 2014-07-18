package jawn
package support.json4s

import org.json4s.JsonAST._

object Parser extends SupportParser[JValue] {

  implicit val facade: Facade[JValue] =
    new Facade[JValue] {
      def jnull() = JNull
      def jfalse() = JBool(false)
      def jtrue() = JBool(true)
      def jnum(s: String) = JDouble(java.lang.Double.parseDouble(s))
      def jint(s: String) = JDouble(java.lang.Double.parseDouble(s))
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
          var vs = List.empty[JValue]
          def add(s: String) { vs = jstring(s) :: vs }
          def add(v: JValue) { vs = v :: vs }
          def finish: JValue = JArray(vs)
          def isObj: Boolean = false
        }

      def objectContext() =
        new FContext[JValue] {
          var key: String = null
          var vs = List.empty[JField]
          def add(s: String): Unit =
            if (key == null) key = s
            else { vs = JField(key, jstring(s)) :: vs; key = null }
          def add(v: JValue): Unit =
            { vs = JField(key, v) :: vs; key = null }
          def finish: JValue = JObject(vs)
          def isObj: Boolean = true
        }
    }
}
