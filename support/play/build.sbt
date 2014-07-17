name := "play-support"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.3.0"
)

seq(bintrayResolverSettings: _*)

seq(bintrayPublishSettings: _*)
