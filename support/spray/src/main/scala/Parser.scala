package org.typelevel.jawn
package support.spray

import spray.json._

object Parser extends SupportParser[JsValue] {
  implicit val facade: Facade[JsValue] =
    new Facade.SimpleFacade[JsValue] {
      def jnull: JsValue = JsNull
      def jfalse: JsValue = JsFalse
      def jtrue: JsValue = JsTrue
      def jnum(s: CharSequence, decIndex: Int, expIndex: Int): JsValue = JsNumber(s.toString)
      def jstring(s: CharSequence): JsValue = JsString(s.toString)
      def jarray(vs: List[JsValue]): JsValue = JsArray(vs: _*)
      def jobject(vs: Map[String, JsValue]): JsValue = JsObject(vs)
    }
}
