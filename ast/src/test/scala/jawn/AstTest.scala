package jawn
package ast

import org.scalatest.matchers.ShouldMatchers
import org.scalatest._
import prop._

import scala.collection.mutable
import scala.util.{Try, Success}

import ArbitraryUtil._

class AstTest extends PropSpec with Matchers with GeneratorDrivenPropertyChecks {

  property("calling .get never crashes") {
    forAll { (v: JValue, s: String, i: Int) =>
      Try(v.get(i).get(s)).isSuccess shouldBe true
      Try(v.get(s).get(i)).isSuccess shouldBe true
      Try(v.get(i).get(i)).isSuccess shouldBe true
      Try(v.get(s).get(s)).isSuccess shouldBe true
    }
  }

  property(".getX and .asX agree") {
    forAll { (v: JValue) =>
      v.getBoolean shouldBe Try(v.asBoolean).toOption
      v.getString shouldBe Try(v.asString).toOption
      v.getLong shouldBe Try(v.asLong).toOption
      v.getDouble shouldBe Try(v.asDouble).toOption
      v.getBigInt shouldBe Try(v.asBigInt).toOption
      v.getBigDecimal shouldBe Try(v.asBigDecimal).toOption
    }
  }

  property(".getBoolean") {
    forAll((b: Boolean) => JBool(b).getBoolean shouldBe Some(b))
  }

  property(".getString") {
    forAll((s: String) => JString(s).getString shouldBe Some(s))
  }

  property(".getLong") {
    forAll { (n: Long) =>
      JNum(n).getLong shouldBe Some(n)
      JParser.parseUnsafe(n.toString).getLong shouldBe Some(n)
    }
  }

  property(".getDouble") {
    forAll { (n: Double) =>
      JNum(n).getDouble shouldBe Some(n)
      JParser.parseUnsafe(n.toString).getDouble shouldBe Some(n)
    }
  }

  property(".getBigInt") {
    forAll { (n: BigInt) =>
      JNum(n.toString).getBigInt shouldBe Some(n)
      JParser.parseUnsafe(n.toString).getBigInt shouldBe Some(n)
    }
  }

  property(".getBigDecimal") {
    forAll { (n: BigDecimal) =>
      // some BigDecimals are too big for BigDecimal's own constuctor :/
      if (Try(BigDecimal(n.toString)).isSuccess) {
        JNum(n.toString).getBigDecimal shouldBe Some(n)
        JParser.parseUnsafe(n.toString).getBigDecimal shouldBe Some(n)
      }
    }
  }
}
