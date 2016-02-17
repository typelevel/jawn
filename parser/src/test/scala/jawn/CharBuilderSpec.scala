package jawn

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.PropSpec

class CharBuilderSpec extends PropSpec with TypeCheckedTripleEquals with GeneratorDrivenPropertyChecks {

  property("append") {
    forAll { xs: List[Char] =>
      val builder = new CharBuilder
      xs.foreach(builder.append)
      assert(builder.makeString === xs.mkString)
    }
  }

  property("extend") {
    forAll { xs: List[String] =>
      val builder = new CharBuilder
      xs.foreach(builder.extend)
      assert(builder.makeString === xs.mkString)
    }
  }

}
