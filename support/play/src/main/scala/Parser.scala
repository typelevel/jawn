package org.typelevel.jawn
package support.play

import play.api.libs.json._

object Parser extends SupportParser[JsValue] {

  implicit val facade: Facade[JsValue] =
    new Facade.SimpleFacade[JsValue] {
      def jnull: JsValue = JsNull
      val jfalse: JsValue = JsBoolean(false)
      val jtrue: JsValue = JsBoolean(true)

      def jnum(s: CharSequence, decIndex: Int, expIndex: Int): JsValue = JsNumber(BigDecimal(s.toString))
      def jstring(s: CharSequence): JsValue = JsString(s.toString)

      def jarray(vs: List[JsValue]): JsValue = JsArray(vs)
      def jobject(vs: Map[String, JsValue]): JsValue = JsObject(vs)
    }
}
