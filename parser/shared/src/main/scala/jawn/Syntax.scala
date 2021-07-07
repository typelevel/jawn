package org.typelevel.jawn

import java.nio.ByteBuffer
import scala.util.Try

object Syntax extends SyntaxPlatform {
  implicit def unitFacade: Facade[Unit] = Facade.NullFacade

  def checkString(s: String): Boolean =
    Try(new StringParser(s).parse()).isSuccess

  def checkByteBuffer(buf: ByteBuffer): Boolean =
    Try(new ByteBufferParser(buf).parse()).isSuccess
}
