package jawn

import java.io.File

import scala.util.{Success, Failure}

object Run {
  def path = "qux3.json"

  def usage() = println("usage: jawn | smart")

  def main(args: Array[String]) {
    args.foreach {
      path =>
      println("jawn: parsing %s" format path)
      val f = new File(path)
      val t0 = System.currentTimeMillis()
      val j = JParser.parseFromFile(f) match {
        case Success(j) => j
        case Failure(e) => throw e
      }
      val t = System.currentTimeMillis - t0
      println("jawn: finished in %d ms" format t)
    }
  }
}
