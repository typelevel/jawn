package jawn

// TODO: consider adding a reset() method and only instantiating one
// of these things.

/**
 * CharBuilder is a specialized way to build Strings.
 * 
 * It wraps a (growable) array of characters, and can accept
 * additional String or Char data to be added to its buffer.
 */
private[jawn] final class CharBuilder {
  @inline final def INITIALSIZE = 8

  private var cs = new Array[Char](INITIALSIZE)
  private var capacity = INITIALSIZE
  private var len = 0

  def makeString: String = new String(cs, 0, len)

  def resizeIfNecessary(goal: Int) {
    if (goal <= capacity) return ()
    var cap = capacity
    while (goal > capacity) cap *= 2
    if (cap > capacity) {
      val ncs = new Array[Char](cap)
      System.arraycopy(cs, 0, ncs, 0, capacity)
      cs = ncs
      capacity = cap
    } else if (cap < capacity) {
      sys.error("maximum string size exceeded")
    }
  }

  def extend(s: String) {
    val tlen = len + s.length
    resizeIfNecessary(tlen)
    var i = len
    while (i < tlen) { cs(i) = s.charAt(i); i += 1 }
  }

  def append(c: Char) {
    resizeIfNecessary(len + 1)
    cs(len) = c
    len += 1
  }
}
