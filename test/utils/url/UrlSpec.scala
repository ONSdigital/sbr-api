package utils.url

import org.scalatest.{ FreeSpec, Matchers }

class UrlSpec extends FreeSpec with Matchers {
  "A Url" - {
    "can be assembled from a baseUrl and a path" in {
      val url = Url(withBase = BaseUrl("protocol", "host", port = 4321), withPath = "foo/bar/baz")

      url shouldBe "protocol://host:4321/foo/bar/baz"
    }
  }
}
