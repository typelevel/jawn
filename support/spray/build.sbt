name := "spray-support"

resolvers += "spray" at "http://repo.spray.io/"

libraryDependencies ++= Seq(
  "io.spray" %% "spray-json" % "1.3.2"
)
