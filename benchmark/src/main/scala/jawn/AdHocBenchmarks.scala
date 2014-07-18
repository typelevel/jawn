package jawn
package benchmark

import scala.collection.mutable

object AdHocBenchmarks {
  def warmups = 2
  def runs = 5

  def json4sNativeParse(path: String) = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    val file = new java.io.File(path)
    val bytes = new Array[Byte](file.length.toInt)
    val fis = new java.io.FileInputStream(file)
    fis.read(bytes)
    val s = new String(bytes, "UTF-8")
    parse(s)
  }

  def json4sJacksonParse(path: String) = {
    import org.json4s._
    import org.json4s.jackson.JsonMethods._
    val file = new java.io.File(path)
    val bytes = new Array[Byte](file.length.toInt)
    val fis = new java.io.FileInputStream(file)
    fis.read(bytes)
    val s = new String(bytes, "UTF-8")
    parse(s)
  }

  def json4sJawnParse(path: String) = {
    val file = new java.io.File(path)
    jawn.support.json4s.Parser.parseFromFile(file).get
  }

  def playParse(path: String) = {
    val file = new java.io.File(path)
    val bytes = new Array[Byte](file.length.toInt)
    val fis = new java.io.FileInputStream(file)
    fis.read(bytes)
    val s = new String(bytes, "UTF-8")
    play.api.libs.json.Json.parse(s)
  }

  def playJawnParse(path: String) = {
    val file = new java.io.File(path)
    jawn.support.play.Parser.parseFromFile(file).get
  }

  def sprayParse(path: String) = {
    val file = new java.io.File(path)
    val bytes = new Array[Byte](file.length.toInt)
    val fis = new java.io.FileInputStream(file)
    fis.read(bytes)
    val s = new String(bytes, "UTF-8")
    spray.json.JsonParser(s)
  }

  def rojomaParse(path: String) = {
    val file = new java.io.File(path)
    val r = new java.io.FileReader(file)
    val br = new java.io.BufferedReader(r)
    com.rojoma.json.io.JsonReader(br).read()
  }

  def rojomaFastParse(path: String) = {
    val file = new java.io.File(path)
    val r = new java.io.FileReader(file)
    val events = new com.rojoma.json.io.FusedBlockJsonEventIterator(r, blockSize = 100000)
    com.rojoma.json.io.JsonReader.fromEvents(events)
  }

  def argonautParse(path: String) = {
    val file = new java.io.File(path)
    val bytes = new Array[Byte](file.length.toInt)
    val fis = new java.io.FileInputStream(file)
    fis.read(bytes)
    val s = new String(bytes, "UTF-8")
    argonaut.Parse.parse(s)
  }

  def sprayScalastuffParse(path: String) = {
    val file = new java.io.File(path)
    val bytes = new Array[Byte](file.length.toInt)
    val fis = new java.io.FileInputStream(file)
    fis.read(bytes)
    val s = new String(bytes, "UTF-8")
    org.scalastuff.json.spray.SprayJsonParser.parse(s)
  }

  def smartJsonParse(path: String) = {
    val file = new java.io.File(path)
    val reader = new java.io.FileReader(file)
    net.minidev.json.JSONValue.parse(reader)
  }

  def parboiledJsonParse(path: String) = {
    val file = new java.io.File(path)
    val bytes = new Array[Byte](file.length.toInt)
    val fis = new java.io.FileInputStream(file)
    fis.read(bytes)
    val s = new String(bytes, "UTF-8")
    new ParboiledParser(s).Json.run().get
  }

  def jacksonParse(path: String) = {
    import com.fasterxml.jackson.databind.ObjectMapper
    import com.fasterxml.jackson.databind.JsonNode
    val file = new java.io.File(path)
    new ObjectMapper().readValue(file, classOf[JsonNode])
  }

  // def liftJsonParse(path: String) = {
  //   val file = new java.io.File(path)
  //   val reader = new java.io.FileReader(file)
  //   net.liftweb.json.JsonParser.parse(reader)
  // }

  def jawnParse(path: String) = {
    val file = new java.io.File(path)
    jawn.ast.JParser.parseFromFile(file).get
  }

  def jawnStringParse(path: String) = {
    val file = new java.io.File(path)
    val bytes = new Array[Byte](file.length.toInt)
    val fis = new java.io.FileInputStream(file)
    fis.read(bytes)
    val s = new String(bytes, "UTF-8")
    jawn.ast.JParser.parseFromString(s).get
  }

  def argonautJawnParse(path: String) = {
    val file = new java.io.File(path)
    jawn.support.argonaut.Parser.parseFromFile(file).get
  }

  def sprayJawnParse(path: String) = {
    val file = new java.io.File(path)
    jawn.support.spray.Parser.parseFromFile(file).get
  }

  def rojomaJawnParse(path: String) = {
    val file = new java.io.File(path)
    jawn.support.rojoma.Parser.parseFromFile(file).get
  }

  def gsonParse(path: String) = {
    val p = new com.google.gson.JsonParser()
    val r = new java.io.BufferedReader(new java.io.FileReader(path))
    p.parse(r)
  }

  def test[A](name: String, path: String)(f: String => A): Double = {
    var h = 0
    (0 until warmups).foreach { _ =>
      val result = f(path)
      h = h ^ result.##
      System.gc()
    }

    var t = 0.0
    (0 until runs).foreach { _ =>
      val t0 = System.nanoTime()
      val result = f(path)
      t += (System.nanoTime() - t0).toDouble / 1000000
      h = h ^ result.##
      System.gc()
    }
    t / runs
  }

  def run[A](name: String, path: String)(f: String => A) {
    try {
      val t = test(name, path)(f)
      println("  %-18s  %10.2f ms" format (name, t))
    } catch {
      case e: Exception =>
      println("  %-18s  %10s" format (name, "FAIL"))
      println(e)
    }
  }

  def main(args: Array[String]) {
    val d = new java.io.File("src/main/resources")
    val xs = d.listFiles.filter(_.getName.endsWith(".json")).sorted
    val fs = if (args.isEmpty) xs else xs.filter(f => args.contains(f.getName))
    
    fs.foreach { f =>
      val path = f.getPath
      val bytes = f.length

      val (size, units) = if (bytes >= 1048576)
        (bytes / 1048576.0, "M")
      else if (bytes >= 1024.0)
        (bytes / 1024.0, "K")
      else
        (bytes / 1.0, "B")

      println("%s (%.1f%s)" format (f.getName, size, units))

      // run("lift-json", path)(liftJsonParse) // buggy, fails to parse, etc

      // run("parboiled-json", path)(parboiledJsonParse)
      // run("smart-json", path)(smartJsonParse)
      // run("json4s-native", path)(json4sNativeParse)
      // run("json4s-jackson", path)(json4sJacksonParse)
      // run("json4s-jawn", path)(json4sJawnParse)
      // run("play", path)(playParse)
      // run("play-jawn", path)(playJawnParse)
      run("rojoma", path)(rojomaParse)
      run("rojoma-fast", path)(rojomaFastParse)
      run("rojoma-jawn", path)(rojomaJawnParse)
      // run("argonaut", path)(argonautParse)
      // run("argonaut-jawn", path)(argonautJawnParse)
      // run("spray", path)(sprayParse)
      // run("spray-scalastuff", path)(sprayScalastuffParse)
      // run("spray-jawn", path)(sprayJawnParse)
      // run("jackson", path)(jacksonParse)
      // run("gson", path)(gsonParse)
      run("jawn", path)(jawnParse)
      run("jawn-string", path)(jawnStringParse)
    }
  }
}
