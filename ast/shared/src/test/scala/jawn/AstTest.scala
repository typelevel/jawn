/*
 * Copyright (c) 2022 Typelevel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.typelevel.jawn
package ast

import org.scalacheck.{Prop, Properties}
import scala.util.{Success, Try}

import ArbitraryUtil._
import Prop.forAll

class AstTest extends Properties("AstTest") with AstTestPlatform {

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
