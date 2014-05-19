import sbt._
import sbt.Keys._

object JawnBuild extends Build {

  lazy val noPublish = Seq(
    publish := (),
    publishLocal := (),
    publishArtifact := false)

  lazy val parser = Project("parser", file("parser"))

  lazy val ast = Project("ast", file("ast")).
    dependsOn(parser)

  lazy val benchmark = Project("benchmark", file("benchmark")).
    dependsOn(ast).
    settings(noPublish: _*)

  lazy val root = Project("jawn", file(".")).
    aggregate(parser, ast).
    settings(noPublish: _*)
}
