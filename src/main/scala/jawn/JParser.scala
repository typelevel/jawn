package jawn

import java.io.File
import java.nio.ByteBuffer

object JParser {

  def parseUnsafe[J](s: String)(implicit facade: Facade[J]): J = new StringParser(s).parse()

  type Result[A] = Either[Exception, A]

  def parseFromString[J](s: String)(implicit facade: Facade[J]): Result[J] = try {
    Right(new StringParser[J](s).parse)
  } catch {
    case (e: Exception) => Left(e)
  }

  def parseFromFile[J](file: File)(implicit facade: Facade[J]): Result[J] = try {
    Right(ChannelParser.fromFile[J](file).parse)
  } catch {
    case (e: Exception) => Left(e)
  }

  def parseFromByteBuffer[J](buf: ByteBuffer)(implicit facade: Facade[J]): Result[J] = try {
    Right(new ByteBufferParser[J](buf).parse)
  } catch {
    case (e: Exception) => Left(e)
  }

  def parseManyFromString[J](str: String)(implicit facade: Facade[J]): Result[Seq[J]] = try {
    Right(new StringParser[J](str).parseMany)
  } catch {
    case (e: Exception) => Left(e)
  }

  def parseManyFromFile[J](file: File)(implicit facade: Facade[J]): Result[Seq[J]] = try {
    Right(ChannelParser.fromFile[J](file).parseMany)
  } catch {
    case (e: Exception) => Left(e)
  }

  def parseManyFromByteBuffer[J](buf: ByteBuffer)(implicit facade: Facade[J]): Result[Seq[J]] = try {
    Right(new ByteBufferParser[J](buf).parseMany)
  } catch {
    case (e: Exception) => Left(e)
  }
}
