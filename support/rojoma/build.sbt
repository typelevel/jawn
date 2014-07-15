name := "rojoma-support"

libraryDependencies ++= Seq(
  "com.rojoma" %% "rojoma-json" % "2.4.3"
)

seq(bintrayResolverSettings: _*)

seq(bintrayPublishSettings: _*)
