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
      val vs = mutable.ArrayBuffer.empty[argonaut.Json]
      def add(s: String) { vs.append(jstring(s)) }
      def add(v: argonaut.Json) { vs.append(v) }
      def finish: argonaut.Json = argonaut.Json.jArray(vs.toList)
      def isObj: Boolean = false
    }

    def objectContext() = new FContext[argonaut.Json] {
      var key: String = null
      var vs = scalaz.InsertionMap.empty[String, argonaut.Json]
      //val vs = mutable.Map.empty[String, argonaut.Json]
      def add(s: String): Unit = if (key == null) {
        key = s
      } else {
        //vs(key) = jstring(s)
        vs = vs ^+^ (key, jstring(s))
        key = null
      }

      def add(v: argonaut.Json): Unit = {
        //vs(key) = v
        vs = vs ^+^ (key, v)
        key = null
      }

      def finish = argonaut.Json.jObjectMap(vs)
      def isObj = true
    }

    def jnull() = argonaut.Json.jNull
    def jfalse() = argonaut.Json.jFalse
    def jtrue() = argonaut.Json.jTrue
    def jnum(s: String) = argonaut.Json.jNumberOrNull(s.toDouble)
    def jstring(s: String) = argonaut.Json.jString(s)
  }
}

object AdHocBenchmarks {
  @inline final def warmups = 2
  @inline final def runs = 5

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
      run("rojoma", path)(argonautParse)
      run("argonaut", path)(argonautParse)
      run("smart-json", path)(smartJsonParse)
      run("jackson", path)(jacksonParse)
      run("jawn", path)(jawnParse)
      run("argojawn", path)(jawnParse)
    }
  }
}
