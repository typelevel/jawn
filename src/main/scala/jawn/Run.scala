package jawn

import java.io.File

object Run {
  def path = "qux3.json"

  def usage() = println("usage: jawn | smart")

  def main(args: Array[String]) {
    implicit val facade = Facade.Jawn

    args.foreach {
      path =>
      println("jawn: parsing %s" format path)
      val f = new File(path)
      val t0 = System.currentTimeMillis()
      val j = JParser.parseFromFile[JValue](f) match {
        case Right(j) => j
        case Left(e) => throw e
      }
      val t = System.currentTimeMillis - t0
      println("jawn: finished in %d ms" format t)
    }
  }
}
