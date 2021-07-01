package org.typelevel.jawn

import java.io.File
import java.nio.channels.ReadableByteChannel
import scala.util.Try

private[jawn] trait SyntaxPlatform { self: Syntax.type =>

  def checkPath(path: String): Boolean =
    Try(ChannelParser.fromFile(new File(path)).parse()).isSuccess

  def checkFile(file: File): Boolean =
    Try(ChannelParser.fromFile(file).parse()).isSuccess

  def checkChannel(ch: ReadableByteChannel): Boolean =
    Try(ChannelParser.fromChannel(ch).parse()).isSuccess
}
