version := "0.4.0"

scalaVersion := "2.10.4"

seq(bintrayResolverSettings: _*)

crossScalaVersions := Seq("2.10.4", "2.11.0")

scalacOptions ++= Seq(
  "-Yinline-warnings",
  "-deprecation",
  "-optimize",
  "-unchecked"
)

// libraryDependencies ++= Seq(
//   "org.scalatest" %% "scalatest" % "2.1.6" % "test",
//   "org.scalacheck" %% "scalacheck" % "1.11.4" % "test"
// )

seq(bintrayPublishSettings: _*)

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
