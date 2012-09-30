package jawn

final class CharBuilder {
  @inline final def INITIALSIZE = 16

  private var cs = new Array[Char](INITIALSIZE)
  private var capacity = INITIALSIZE
  private var len = 0

  def makeString: String = new String(cs, 0, len)

  def append(c: Char) {
    if (len == capacity) {
      val n = capacity * 2
      val ncs = new Array[Char](n)
      System.arraycopy(cs, 0, ncs, 0, capacity)
      cs = ncs
      capacity = n
    }
    cs(len) = c
    len += 1
  }
}
