import ReleaseTransformations._

lazy val jawnSettings = Seq(
  organization := "org.spire-math",
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.0"),

  resolvers += Resolver.sonatypeRepo("releases"),
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.0" % "test",
    "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
  ),
  scalacOptions ++= Seq(
    //"-Yinline-warnings",
    "-deprecation",
    "-optimize",
    "-unchecked"
  ),

  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  homepage := Some(url("http://github.com/non/jawn")),

  // release stuff
  releaseCrossBuild := true,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := Function.const(false),

  publishTo <<= (version).apply { v =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("Snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("Releases" at nexus + "service/local/staging/deploy/maven2")
  },

  pomExtra := (
    <scm>
      <url>git@github.com:non/jawn.git</url>
      <connection>scm:git:git@github.com:non/jawn.git</connection>
    </scm>
    <developers>
      <developer>
        <id>d_m</id>
        <name>Erik Osheim</name>
        <url>http://github.com/non/</url>
      </developer>
    </developers>
  ),

  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    ReleaseHelper.runCommandAndRemaining("+test"), // formerly runTest
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    ReleaseHelper.runCommandAndRemaining("+publishSigned"),
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges))

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false)

lazy val root = project.in(file("."))
  .aggregate(all.map(Project.projectToRef): _*)
  .enablePlugins(CrossPerProjectPlugin)
  .disablePlugins(JmhPlugin)
  .settings(name := "jawn")
  .settings(jawnSettings: _*)
  .settings(noPublish: _*)

lazy val parser = project.in(file("parser"))
  .settings(name := "parser")
  .settings(moduleName := "jawn-parser")
  .settings(jawnSettings: _*)
  .disablePlugins(JmhPlugin)

lazy val ast = project.in(file("ast"))
  .dependsOn(parser % "compile->compile;test->test")
  .settings(name := "ast")
  .settings(moduleName := "jawn-ast")
  .settings(jawnSettings: _*)
  .disablePlugins(JmhPlugin)

def support(s: String) =
  Project(id = s, base = file(s"support/$s"))
    .settings(name := (s + "-support"))
    .settings(moduleName := "jawn-" + s)
    .dependsOn(parser)
    .settings(jawnSettings: _*)
    .disablePlugins(JmhPlugin)

lazy val supportArgonaut = support("argonaut")
  .settings(crossScalaVersions := Seq("2.10.6", "2.11.8"))
  .settings(libraryDependencies += "io.argonaut" %% "argonaut" % "6.1")

lazy val supportJson4s = support("json4s")
  .settings(crossScalaVersions := Seq("2.10.6", "2.11.8"))
  .settings(libraryDependencies += "org.json4s" %% "json4s-ast" % "3.4.2")

lazy val supportPlay = support("play")
  .settings(crossScalaVersions := Seq("2.10.6", "2.11.8"))
  .settings(libraryDependencies += (scalaBinaryVersion.value match {
    case "2.10" => "com.typesafe.play" %% "play-json" % "2.4.8"
    case _ =>  "com.typesafe.play" %% "play-json" % "2.5.8"
  }))

lazy val supportRojoma = support("rojoma")
  .settings(crossScalaVersions := Seq("2.10.6", "2.11.8"))
  .settings(libraryDependencies += "com.rojoma" %% "rojoma-json" % "2.4.3")

lazy val supportRojomaV3 = support("rojoma-v3")
  .settings(crossScalaVersions := Seq("2.10.6", "2.11.8"))
  .settings(libraryDependencies += "com.rojoma" %% "rojoma-json-v3" % "3.3.0")

lazy val supportSpray = support("spray")
  .settings(resolvers += "spray" at "http://repo.spray.io/")
  .settings(libraryDependencies += "io.spray" %% "spray-json" % "1.3.2")

lazy val benchmark = project.in(file("benchmark"))
  .dependsOn(all.map(Project.classpathDependency[Project]): _*)
  .settings(name := "jawn-benchmark")
  .settings(jawnSettings: _*)
  .settings(scalaVersion := "2.11.8")
  .settings(noPublish: _*)
  .settings(crossScalaVersions := Seq("2.11.8"))
  .enablePlugins(JmhPlugin)

lazy val all =
  Seq(parser, ast, supportArgonaut, supportJson4s, supportPlay, supportRojoma, supportRojomaV3, supportSpray)
