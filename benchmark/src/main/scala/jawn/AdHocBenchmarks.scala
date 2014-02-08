package jawn

import scala.collection.mutable

object Xyz {
  val Argonaut = new Facade[argonaut.Json] {

    def singleContext() = new FContext[argonaut.Json] {
      var value: argonaut.Json = null
      def add(s: String) { value = jstring(s) }
      def add(v: argonaut.Json) { value = v }
      def finish: argonaut.Json = value
      def isObj: Boolean = false
    }

    def arrayContext() = new FContext[argonaut.Json] {
      val vs = mutable.ListBuffer.empty[argonaut.Json]
      def add(s: String) { vs.append(jstring(s)) }
      def add(v: argonaut.Json) { vs.append(v) }
      def finish: argonaut.Json = argonaut.Json.jArray(vs.toList)
      def isObj: Boolean = false
    }

    def objectContext() = new FContext[argonaut.Json] {
      var key: String = null
      var vs = argonaut.JsonObject.empty
      def add(s: String): Unit = if (key == null) {
        key = s
      } else {
        vs = vs + (key, jstring(s))
        key = null
      }

      def add(v: argonaut.Json): Unit = {
        vs = vs + (key, v)
        key = null
      }

      def finish = argonaut.Json.jObject(vs)
      def isObj = true
    }

    def jnull() = argonaut.Json.jNull
    def jfalse() = argonaut.Json.jFalse
    def jtrue() = argonaut.Json.jTrue
    def jnum(s: String) = argonaut.Json.jNumberOrNull(java.lang.Double.parseDouble(s))
    def jint(s: String) = argonaut.Json.jNumberOrNull(java.lang.Integer.parseInt(s))
    // use the following to simulate deferred parseInt/parseDouble
    // def jnum(s: String) = jstring(s)
    // def jint(s: String) = jstring(s)
    def jstring(s: String) = argonaut.Json.jString(s)
  }
}

object AdHocBenchmarks {
  @inline final def warmups = 2
  @inline final def runs = 5

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

  def playParse(path: String) = {
    val file = new java.io.File(path)
    val bytes = new Array[Byte](file.length.toInt)
    val fis = new java.io.FileInputStream(file)
    fis.read(bytes)
    val s = new String(bytes, "UTF-8")
    play.api.libs.json.Json.parse(s)
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

  def argonautParse(path: String) = {
    val file = new java.io.File(path)
    val bytes = new Array[Byte](file.length.toInt)
    val fis = new java.io.FileInputStream(file)
    fis.read(bytes)
    val s = new String(bytes, "UTF-8")
    argonaut.Parse.parse(s)
  }

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
    val file = new java.io.File(path)
    jawn.JParser.parseFromFile(file).right.get
  }

  def argojawnParse(path: String) = {
    implicit val facade = Xyz.Argonaut
    val file = new java.io.File(path)
    jawn.GenericParser.parseFromFile[argonaut.Json](file).right.get
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
    }
  }

  def main(args: Array[String]) {
    val d = new java.io.File("src/main/resources")
    val xs = d.listFiles.filter(_.getName.endsWith(".json")).sorted
    val fs = if (args.isEmpty) xs else xs.filter(f => args.contains(f.getName))
    
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
      run("json4s-native", path)(json4sNativeParse)
      run("json4s-jackson", path)(json4sJacksonParse)
      run("play", path)(playParse)
      run("spray", path)(sprayParse)
      run("rojoma", path)(rojomaParse)
      run("argonaut", path)(argonautParse)
      run("smart-json", path)(smartJsonParse)
      run("jackson", path)(jacksonParse)
      run("gson", path)(gsonParse)
      run("jawn", path)(jawnParse)
      run("argonaut-jawn", path)(argojawnParse)
      // run("lift-json", path)(liftJsonParse) // buggy, fails to parse, etc
    }
  }
}
