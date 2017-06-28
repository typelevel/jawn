package jawn

object Util {

  case class InvalidLong(s: String) extends Exception(s"string '$s' not a valid Long")

  def parseLong(cs: CharSequence): Long = {

    // we store the inverse of the positive sum, to ensure we don't
    // incorrectly overflow on Long.MinValue. for positive numbers
    // this inverse sum will be inverted before being returned.
    var inverseSum: Long = 0L
    var inverseSign: Long = -1L
    var i: Int = 0

    if (cs.charAt(0) == '-') {
      inverseSign = 1L
      i = 1
    }

    val len = cs.length
    val size = len - i
    if (i >= len) throw InvalidLong(cs.toString)
    if (size > 19) throw InvalidLong(cs.toString)

    while (i < len) {
      val digit = cs.charAt(i).toInt - 48
      if (digit < 0 || 9 < digit) throw InvalidLong(cs.toString)
      inverseSum = inverseSum * 10L - digit
      i += 1
    }

    // detect and throw on overflow
    if (size == 19 && (inverseSum >= 0 || (inverseSum == Long.MinValue && inverseSign < 0))) {
      throw InvalidLong(cs.toString)
    }

    inverseSum * inverseSign
  }

  /**
   * Parse a Long from a given CharSequence.
   *
   * This method assumes the input has already been validated, and is
   * a valid base-10 integer (-?[1-9][0-9]*|0). This method is not
   * guaranteed to fail on invalid input (although it may fail).
   *
   * In particular, this method will not correctly handle:
   *
   *  - empty sequences
   *  - leading zeros (e.g. 012)
   *  - leading plus (e.g. +33)
   *  - decimals or exponents (e.g. 2.3, 1e4)
   *  - other bases (e.g. a1ef, 0x44ed)
   *
   * This method should only be used on sequences which have already
   * been parsed (e.g. by a Jawn parser).
   */
  def parseLongUnsafe(cs: CharSequence): Long = {

    // we store the inverse of the positive sum, to ensure we don't
    // incorrectly overflow on Long.MinValue. for positive numbers
    // this inverse sum will be inverted before being returned.
    var inverseSum: Long = 0L
    var inverseSign: Long = -1L
    var i: Int = 0

    if (cs.charAt(0) == '-') {
      inverseSign = 1L
      i = 1
    }

    val len = cs.length
    while (i < len) {
      inverseSum = inverseSum * 10L - (cs.charAt(i).toInt - 48)
      i += 1
    }

    inverseSum * inverseSign
  }
}
