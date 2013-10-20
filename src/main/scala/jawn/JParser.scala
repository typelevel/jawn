package jawn

import java.io.File
import java.nio.ByteBuffer

object JParser {

  def parseUnsafe(s: String): JValue = new StringParser(s).parse()

  type Result[A] = Either[Exception, A]

  def parseFromString(s: String): Result[JValue] = try {
    Right(new StringParser(s).parse())
  } catch {
    case (e: Exception) => Left(e)
  }

  def parseFromFile(file: File): Result[JValue] = try {
    Right(ChannelParser.fromFile(file).parse())
  } catch {
    case (e: Exception) => Left(e)
  }

  def parseFromByteBuffer(buf: ByteBuffer): Result[JValue] = try {
    Right(new ByteBufferParser(buf).parse())
  } catch {
    case (e: Exception) => Left(e)
  }

  def parseManyFromString(str: String): Result[Seq[JValue]] = try {
    Right(new StringParser(str).parseMany())
  } catch {
    case (e: Exception) => Left(e)
  }

  def parseManyFromFile(file: File): Result[Seq[JValue]] = try {
    Right(ChannelParser.fromFile(file).parseMany())
  } catch {
    case (e: Exception) => Left(e)
  }

  def parseManyFromByteBuffer(buf: ByteBuffer): Result[Seq[JValue]] = try {
    Right(new ByteBufferParser(buf).parseMany())
  } catch {
    case (e: Exception) => Left(e)
  }
}
