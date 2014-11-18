package jawn
package support.spray

import spray.json._

object Parser extends SupportParser[JsValue] {
  implicit val facade: Facade[JsValue] =
    new SimpleFacade[JsValue] {
      def jnull() = JsNull
      def jfalse() = JsFalse
      def jtrue() = JsTrue
      def jnum(s: String) = JsNumber(s)
      def jint(s: String) = JsNumber(s)
      def jstring(s: String) = JsString(s)
      def jarray(vs: List[JsValue]) = JsArray(vs: _*)
      def jobject(vs: Map[String, JsValue]) = JsObject(vs)
    }
}
