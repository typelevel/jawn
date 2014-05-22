import sbt._
import sbt.Keys._

object JawnBuild extends Build {

  lazy val noPublish = Seq(
    publish := (),
    publishLocal := (),
    publishArtifact := false)

  lazy val testDeps =
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "2.1.6" % "test",
      "org.scalacheck" %% "scalacheck" % "1.11.4" % "test"
    )

  lazy val parser = Project("parser", file("parser")).
    settings(testDeps)

  lazy val ast = Project("ast", file("ast")).
    settings(testDeps).
    dependsOn(parser)

  lazy val benchmark = Project("benchmark", file("benchmark")).
    settings(noPublish: _*).
    dependsOn(ast)

  lazy val root = Project("jawn", file(".")).
    settings(noPublish: _*).
    aggregate(parser, ast)
}
