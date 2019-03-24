package org.typelevel.jawn

import claimant.Claim
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll

class CharBuilderSpec extends Properties("CharBuilderSpec") {

  property("append") =
    forAll { xs: List[Char] =>
      val builder = new CharBuilder
      xs.foreach(builder.append)
      Claim(builder.makeString == xs.mkString)
    }

  property("extend") =
    forAll { xs: List[String] =>
      val builder = new CharBuilder
      xs.foreach(builder.extend)
      Claim(builder.makeString == xs.mkString)
    }
}
