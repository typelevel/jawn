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

import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import scala.util.Try

// Had to be duplicated in whole (instead of "platformed") for bincompat
trait SupportParser[J] {
  implicit def facade: Facade[J]

  def parseUnsafe(s: String): J =
    new StringParser(s).parse()

  def parseFromString(s: String): Try[J] =
    Try(new StringParser[J](s).parse())

  def parseFromCharSequence(cs: CharSequence): Try[J] =
    Try(new CharSequenceParser[J](cs).parse())

  def parseFromPath(path: String): Try[J] =
    Try(ChannelParser.fromFile[J](new File(path)).parse())

  def parseFromFile(file: File): Try[J] =
    Try(ChannelParser.fromFile[J](file).parse())

  def parseFromChannel(ch: ReadableByteChannel): Try[J] =
    Try(ChannelParser.fromChannel[J](ch).parse())

  def parseFromByteBuffer(buf: ByteBuffer): Try[J] =
    Try(new ByteBufferParser[J](buf).parse())

  def parseFromByteArray(src: Array[Byte]): Try[J] =
    Try(new ByteArrayParser[J](src).parse())

  def async(mode: AsyncParser.Mode): AsyncParser[J] =
    AsyncParser[J](mode)
}
