package utils.url

import org.scalatest.{ FreeSpec, Matchers }
import utils.url.BaseUrl.asUrlString

class BaseUrlSpec extends FreeSpec with Matchers {
  "A BaseUrl" - {
    "can be represented as a URL string" in {
      asUrlString(BaseUrl("protocol", "hostname", port = 1234)) shouldBe "protocol://hostname:1234"
    }
  }
}
