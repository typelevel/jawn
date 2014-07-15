name := "play-support"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.2.1"
)

seq(bintrayResolverSettings: _*)

seq(bintrayPublishSettings: _*)
