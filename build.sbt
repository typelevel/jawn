import ReleaseTransformations._

lazy val jawnSettings = Seq(
  organization := "org.spire-math",
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.10.5", "2.11.7"),

  resolvers += Resolver.sonatypeRepo("releases"),
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "2.2.5" % "test",
    "org.scalacheck" %% "scalacheck" % "1.12.4" % "test"
  ),
  scalacOptions ++= Seq(
    "-Yinline-warnings",
    "-deprecation",
    "-optimize",
    "-unchecked"
  ),

  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  homepage := Some(url("http://github.com/non/jawn")),

  // release stuff
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
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
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
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
  .disablePlugins(JmhPlugin)
  .settings(name := "jawn")
  .settings(jawnSettings: _*)
  .settings(noPublish: _*)

lazy val parser = project.in(file("parser"))
  .disablePlugins(JmhPlugin)
  .settings(name := "parser")
  .settings(moduleName := "jawn-parser")
  .settings(jawnSettings: _*)

lazy val ast = project.in(file("ast"))
  .dependsOn(parser % "compile->compile;test->test")
  .disablePlugins(JmhPlugin)
  .settings(name := "ast")
  .settings(moduleName := "jawn-ast")
  .settings(jawnSettings: _*)

def support(name: String) =
  Project(id = name, base = file(s"support/$name"))
    .dependsOn(parser)
    .disablePlugins(JmhPlugin)
    .settings(moduleName := "jawn-" + name)
    .settings(jawnSettings: _*)

lazy val supportArgonaut = support("argonaut")
lazy val supportJson4s = support("json4s")
lazy val supportPlay = support("play")
lazy val supportRojoma = support("rojoma")
lazy val supportRojomaV3 = support("rojoma-v3")
lazy val supportSpray = support("spray")

lazy val benchmark = project.in(file("benchmark"))
  .dependsOn(all.map(Project.classpathDependency[Project]): _*)
  .enablePlugins(JmhPlugin)
  .settings(name := "jawn-benchmark")
  .settings(jawnSettings: _*)
  .settings(scalaVersion := "2.11.7")
  .settings(noPublish: _*)

lazy val all =
  Seq(parser, ast, supportArgonaut, supportJson4s, supportPlay, supportRojoma, supportRojomaV3, supportSpray)
