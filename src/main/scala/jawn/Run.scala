package jawn

import java.io.File

object Run {
  def path = "qux3.json"

  def usage() = println("usage: jawn | smart")

  def main(args: Array[String]) {
    args.foreach {
      path =>
      println("jawn: parsing %s" format path)
      val f = new File(path)
      val t0 = System.currentTimeMillis()
      val e = JParser.parseFromFile(f)
      val t = System.currentTimeMillis - t0
      println("jawn: finished in %d ms" format t)
    }
  }
}
