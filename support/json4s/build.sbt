name := "json4s-support"

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-native" % "3.2.10"
)

seq(bintrayResolverSettings: _*)

seq(bintrayPublishSettings: _*)
