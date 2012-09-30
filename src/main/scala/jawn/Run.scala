package jawn

object Run {
  def path = "qux3.json"

  def usage() = println("usage: jawn | smart")

  def main(args: Array[String]) {
    args.foreach {
      path =>
      println("jawn: parsing %s" format path)
      val t0 = System.currentTimeMillis()
      val j = new PathParser(path).parse(0)
      val t = System.currentTimeMillis - t0
      println("jawn: finished in %d ms" format t)
    }
  }
}
