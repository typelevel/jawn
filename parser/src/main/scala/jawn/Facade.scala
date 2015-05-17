package jawn

/**
 * Facade is a type class that describes how Jawn should construct
 * JSON AST elements of type J.
 * 
 * Facade[J] also uses FContext[J] instances, so implementors will
 * usually want to define both.
 */
trait Facade[J] {
  def singleContext(): FContext[J]
  def arrayContext(): FContext[J]
  def objectContext(): FContext[J]

  def jnull(): J
  def jfalse(): J
  def jtrue(): J
  def jnum(s: String): J
  def jint(s: String): J
  def jstring(s: String): J
}

/**
 * FContext is used to construct nested JSON values.
 *
 * The most common cases are to build objects and arrays. However,
 * this type is also used to build a single top-level JSON element, in
 * cases where the entire JSON document consists of "333.33".
 */
trait FContext[J] {
  def add(s: String): Unit
  def add(v: J): Unit
  def finish: J
  def isObj: Boolean
}
