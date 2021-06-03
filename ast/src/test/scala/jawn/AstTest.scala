package org.typelevel.jawn
package ast

import org.scalacheck.{Prop, Properties}
import scala.util.{Success, Try}

import ArbitraryUtil._
import Prop.forAll

class AstTest extends Properties("AstTest") {

  property("calling .get never crashes") = forAll { (v: JValue, s: String, i: Int) =>
    Prop(
      Try(v.get(i).get(s)).isSuccess &&
        Try(v.get(s).get(i)).isSuccess &&
        Try(v.get(i).get(i)).isSuccess &&
        Try(v.get(s).get(s)).isSuccess
    )
  }

  property(".getX and .asX agree") = forAll { (v: JValue) =>
    Prop(
      v.getBoolean == Try(v.asBoolean).toOption &&
        v.getString == Try(v.asString).toOption &&
        v.getInt == Try(v.asInt).toOption &&
        v.getLong == Try(v.asLong).toOption &&
        v.getDouble == Try(v.asDouble).toOption &&
        v.getBigInt == Try(v.asBigInt).toOption &&
        v.getBigDecimal == Try(v.asBigDecimal).toOption
    )
  }

  property(".getBoolean") = forAll((b: Boolean) => Prop(JBool(b).getBoolean == Some(b)))

  property(".getString") = forAll((s: String) => Prop(JString(s).getString == Some(s)))

  property(".getInt") = forAll { (n: Int) =>
    Prop(
      JNum(n.toLong).getInt == Some(n) &&
        JParser.parseUnsafe(n.toString).getInt == Some(n)
    )
  }

  property(".getLong") = forAll { (n: Long) =>
    Prop(
      JNum(n).getLong == Some(n) &&
        JParser.parseUnsafe(n.toString).getLong == Some(n)
    )
  }

  property(".getDouble") = forAll { (n: Double) =>
    Prop(
      JNum(n).getDouble == Some(n) &&
        JParser.parseUnsafe(n.toString).getDouble == Some(n)
    )
  }

  property(".getBigInt") = forAll { (n: BigInt) =>
    Prop(
      JNum(n.toString).getBigInt == Some(n) &&
        JParser.parseUnsafe(n.toString).getBigInt == Some(n)
    )
  }

  property(".getBigDecimal") = forAll { (n: BigDecimal) =>
    if (Try(BigDecimal(n.toString)) == Success(n))
      Prop(
        JNum(n.toString).getBigDecimal == Some(n) &&
          JParser.parseUnsafe(n.toString).getBigDecimal == Some(n)
      )
    else
      Prop(true)
  }
}
