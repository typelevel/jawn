name := "play-support"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.5.2"
)

crossScalaVersions := Seq("2.11.7") // Play 2.5.x doesn't support Scala 2.10.x
