// package jawn
// 
// import com.google.caliper.Param
// 
// object ParserBenchmarks extends MyRunner(classOf[ParserBenchmarks])
// 
// class ParserBenchmarks extends MyBenchmark {
//   // any benchmark which takes more than 10s to complete will explode :(
//   @Param(Array("qux1", "qux2", "qux3"))
//   var name: String =  null
//   var path: String = null
// 
//   override protected def setUp() {
//     path = "src/main/resources/%s.json" format name
//   }
// 
//   def timeSmartJson(reps: Int) = run(reps) {
//     val file = new java.io.File(path)
//     val reader = new java.io.FileReader(file)
//     net.minidev.json.JSONValue.parse(reader)
//   }
// 
//   def timeJackson(reps: Int) = run(reps) {
//     import com.fasterxml.jackson.databind.ObjectMapper
//     import com.fasterxml.jackson.databind.JsonNode
//     val file = new java.io.File(path)
//     new ObjectMapper().readValue(file, classOf[JsonNode])
//   }
// 
//   def timeJawn(reps:Int) = run(reps) {
//     new PathParser(path).parse(0)
//   }
// }
