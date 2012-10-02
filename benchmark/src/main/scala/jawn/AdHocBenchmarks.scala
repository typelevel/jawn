package jawn

object AdHocBenchmarks {
  @inline final def warmups = 2
  @inline final def runs = 5

  def smartJsonParse(path: String) = {
    val file = new java.io.File(path)
    val reader = new java.io.FileReader(file)
    net.minidev.json.JSONValue.parse(reader)
  }

  def jacksonParse(path: String) = {
    import com.fasterxml.jackson.databind.ObjectMapper
    import com.fasterxml.jackson.databind.JsonNode
    val file = new java.io.File(path)
    new ObjectMapper().readValue(file, classOf[JsonNode])
  }

  def liftJsonParse(path: String) = {
    val file = new java.io.File(path)
    val reader = new java.io.FileReader(file)
    net.liftweb.json.JsonParser.parse(reader)
  }

  def jawnParse(path: String) = {
    new jawn.PathParser(path).parse(0)
  }

  def test[A](name: String, path:String)(f: String => A): Double = {
    var h = 0
    (0 until warmups).foreach {
      _ =>
      val result = f(path)
      h = h ^ result.##
      System.gc()
    }

    var t = 0L
    (0 until runs).foreach {
      _ =>
      val t0 = System.currentTimeMillis()
      val result = f(path)
      t += System.currentTimeMillis - t0
      h = h ^ result.##
      System.gc()
    }
    t.toDouble / runs
  }

  def run[A](name: String, path: String)(f: String => A) {
    try {
      val t = test(name, path)(f)
      println("  %-18s  %10.1f ms" format (name, t))
    } catch {
      case e: Exception =>
      println("  %-18s  %10s" format (name, "FAIL"))
    }
  }

  def main(args: Array[String]) {
    val d = new java.io.File("src/main/resources")
    val fs = d.listFiles.filter(_.getName.endsWith(".json")).sorted
    
    fs.foreach {
      f =>
      val path = f.getPath
      val bytes = f.length

      val (size, units) = if (bytes >= 1048576)
        (bytes / 1048576.0, "M")
      else if (bytes >= 1024.0)
        (bytes / 1024.0, "K")
      else
        (bytes / 1.0, "B")

      println("%s (%.1f%s)" format (f.getName, size, units))
      //run("lift-json", path)(liftJsonParse)
      run("smart-json", path)(smartJsonParse)
      run("jackson", path)(jacksonParse)
      run("jawn", path)(jawnParse)
    }
  }
}
