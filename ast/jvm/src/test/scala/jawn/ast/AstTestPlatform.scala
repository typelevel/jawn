package org.typelevel.jawn
package ast

import org.scalacheck.Prop

import Prop.forAll

private[jawn] trait AstTestPlatform { self: AstTest =>

  // See https://github.com/typelevel/jawn/issues/353

  property(".getDouble") = forAll { (n: Double) =>
    Prop(
      JNum(n).getDouble == Some(n) &&
        JParser.parseUnsafe(n.toString).getDouble == Some(n)
    )
  }

}
