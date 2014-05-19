package jawn

trait FContext[J] {
  def add(s: String): Unit
  def add(v: J): Unit
  def finish: J
  def isObj: Boolean
}

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
