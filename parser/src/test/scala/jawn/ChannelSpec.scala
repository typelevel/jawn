package jawn
package parser

import org.scalatest._

import java.nio.channels.ByteChannel
import scala.util.Success

class ChannelSpec extends PropSpec with Matchers {

  property("large strings in files are ok") {
    val M = 1000000
    val q = "\""
    val big = q + ("x" * (40 * M)) + q
    val bigEscaped = q + ("\\\\" * (20 * M)) + q

    Util.withTemp(big) { t =>
      Parser.parseFromFile(t)(NullFacade).isSuccess shouldBe true
    }

    Util.withTemp(bigEscaped) { t =>
      Parser.parseFromFile(t)(NullFacade).isSuccess shouldBe true
    }
  }
}
