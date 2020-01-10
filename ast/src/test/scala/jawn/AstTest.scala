package org.typelevel.jawn
package ast

import org.scalacheck.{Prop, Properties}
import org.typelevel.claimant.Claim
import scala.util.{Success, Try}

import ArbitraryUtil._
import Prop.forAll

class AstTest extends Properties("AstTest") {

  property("calling .get never crashes") = forAll { (v: JValue, s: String, i: Int) =>
    Claim(
      Try(v.get(i).get(s)).isSuccess &&
        Try(v.get(s).get(i)).isSuccess &&
        Try(v.get(i).get(i)).isSuccess &&
        Try(v.get(s).get(s)).isSuccess
    )
  }

  property(".getX and .asX agree") = forAll { (v: JValue) =>
    Claim(
      v.getBoolean == Try(v.asBoolean).toOption &&
        v.getString == Try(v.asString).toOption &&
        v.getInt == Try(v.asInt).toOption &&
        v.getLong == Try(v.asLong).toOption &&
        v.getDouble == Try(v.asDouble).toOption &&
        v.getBigInt == Try(v.asBigInt).toOption &&
        v.getBigDecimal == Try(v.asBigDecimal).toOption
    )
  }

  property(".getBoolean") = forAll((b: Boolean) => Claim(JBool(b).getBoolean == Some(b)))

  property(".getString") = forAll((s: String) => Claim(JString(s).getString == Some(s)))

  property(".getInt") = forAll { (n: Int) =>
    Claim(
      JNum(n.toLong).getInt == Some(n) &&
        JParser.parseUnsafe(n.toString).getInt == Some(n)
    )
  }

  property(".getLong") = forAll { (n: Long) =>
    Claim(
      JNum(n).getLong == Some(n) &&
        JParser.parseUnsafe(n.toString).getLong == Some(n)
    )
  }

  property(".getDouble") = forAll { (n: Double) =>
    Claim(
      JNum(n).getDouble == Some(n) &&
        JParser.parseUnsafe(n.toString).getDouble == Some(n)
    )
  }

  property(".getBigInt") = forAll { (n: BigInt) =>
    Claim(
      JNum(n.toString).getBigInt == Some(n) &&
        JParser.parseUnsafe(n.toString).getBigInt == Some(n)
    )
  }

  property(".getBigDecimal") = forAll { (n: BigDecimal) =>
    if (Try(BigDecimal(n.toString)) == Success(n)) {
      Claim(
        JNum(n.toString).getBigDecimal == Some(n) &&
          JParser.parseUnsafe(n.toString).getBigDecimal == Some(n)
      )
    } else {
      Claim(true)
    }
  }
}
