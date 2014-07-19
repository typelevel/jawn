name := "rojoma-v3-support"

libraryDependencies ++= Seq(
  "com.rojoma" %% "rojoma-json-v3" % "3.0.0"
)

seq(bintrayResolverSettings: _*)

seq(bintrayPublishSettings: _*)
