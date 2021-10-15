package org.typelevel.jawn
package parser

import org.scalacheck.Prop

private[jawn] trait SyntaxCheckPlatform { self: SyntaxCheck =>

  def isValidSyntaxPlatform(s: String): Boolean = true

  def testErrorLocPlatform(json: String, line: Int, col: Int): Prop = Prop(true)

}
