import sbt._
import sbt.Keys._

object MyBuild extends Build {
  override lazy val settings = super.settings ++ Seq(
    name := "jawn",
    version := "0.0.1",
    scalaVersion := "2.10.0-M7",

    scalacOptions ++= Seq(
      "-Yinline-warnings",
      "-deprecation",
      "-optimize",
      "-unchecked"
    ),

    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % "2.10.0-M7",
      "org.scalatest" % "scalatest_2.10.0-M7" % "1.9-2.10.0-M7-B1" % "test",
      "debox" % "debox" % "0.1.0" from "http://plastic-idolatry.com/jars/debox_2.10.0-M7-0.1.0.jar"
    )
  )

  val key = AttributeKey[Boolean]("javaOptionsPatched")

  lazy val root = Project("jawn", file("."))

  lazy val benchmark: Project = Project("benchmark", file("benchmark")) settings (benchmarkSettings: _*) dependsOn (root)

  def benchmarkSettings = Seq(
    // raise memory limits here if necessary
    javaOptions in run += "-Xmx6G",

    libraryDependencies ++= Seq(
      "net.liftweb" % "lift-json_2.9.2" % "2.5-M1",
      "com.fasterxml.jackson.core" % "jackson-annotations" % "2.0.6",
      "com.fasterxml.jackson.core" % "jackson-core" % "2.0.6",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.0.6",
      "net.minidev" % "json-smart" % "1.1.1",
      "com.google.guava" % "guava" % "r09",
      "com.google.code.java-allocation-instrumenter" % "java-allocation-instrumenter" % "2.0",
      "com.google.code.caliper" % "caliper" % "1.0-SNAPSHOT" from "http://n0d.es/jars/caliper-1.0-SNAPSHOT.jar",
      "com.google.code.gson" % "gson" % "1.7.1"
    ),

    // enable forking in run
    fork in run := true,

    // custom kludge to get caliper to see the right classpath

    // we need to add the runtime classpath as a "-cp" argument to the
    // `javaOptions in run`, otherwise caliper will not see the right classpath
    // and die with a ConfigurationException. unfortunately `javaOptions` is a
    // SettingsKey and `fullClasspath in Runtime` is a TaskKey, so we need to
    // jump through these hoops here in order to feed the result of the latter
    // into the former
    onLoad in Global ~= { previous => state =>
      previous {
        state.get(key) match {
          case None =>
            // get the runtime classpath, turn into a colon-delimited string
            val classPath = Project.runTask(fullClasspath in Runtime in benchmark, state).get._2.toEither.right.get.files.mkString(":")
            // return a state with javaOptionsPatched = true and javaOptions set correctly
            Project.extract(state).append(Seq(javaOptions in (benchmark, run) ++= Seq("-cp", classPath)), state.put(key, true))

          case Some(_) => state // the javaOptions are already patched
        }
      }
    }

    // caliper stuff stolen shamelessly from scala-benchmarking-template
  )
}
