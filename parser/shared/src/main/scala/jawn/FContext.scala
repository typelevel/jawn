package org.typelevel.jawn

/**
 * FContext is used to construct nested JSON values.
 *
 * The most common cases are to build objects and arrays. However, this type is also used to build a single top-level
 * JSON element, in cases where the entire JSON document consists of "333.33".
 */
trait FContext[J] {
  def add(s: CharSequence, index: Int): Unit
  def add(s: CharSequence, start: Int, limit: Int): Unit = add(s, start)
  def add(v: J, index: Int): Unit
  def finish(index: Int): J
  def isObj: Boolean
}

object FContext {

  /**
   * A convenience trait for implementers who don't need character offsets.
   */
  trait NoIndexFContext[J] extends FContext[J] {
    def add(s: CharSequence): Unit
    def add(v: J): Unit
    def finish(): J

    final def add(s: CharSequence, index: Int): Unit = add(s)
    final def add(v: J, index: Int): Unit = add(v)
    final def finish(index: Int): J = finish()
  }
}
