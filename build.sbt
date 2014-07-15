version := "0.5.0"

scalaVersion := "2.10.4"

seq(bintrayResolverSettings: _*)

crossScalaVersions := Seq("2.10.4", "2.11.1")

scalacOptions ++= Seq(
  "-Yinline-warnings",
  "-deprecation",
  "-optimize",
  "-unchecked"
)

seq(bintrayPublishSettings: _*)

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
