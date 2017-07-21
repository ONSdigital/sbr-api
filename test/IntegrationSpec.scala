package test

import play.api.test.Helpers._

/**
 * Created by haqa on 14/07/2017.
 */
class IntegrationSpec extends TestUtils {

  "Query Response" should {

    "get by anonymous Enterprise id" in {
      val vat = 9900223267L
      val res = fakeRequest(s"/v1/search?id=${vat}")
      status(res) mustBe OK
      contentType(res) mustBe Some("application/json")
      //      val found = getJsValue(contentAsJson(res) \ "vatref")
      //      val source = getJsValue(contentAsJson(res) \ "source")
      //      found must include(vat.toString)
      //      source.toLowerCase mustBe "vat"
    }

    "check if multiple records return" ignore {
      val id = 9900156115L
      val res = fakeRequest(s"/v1/search?id=${id}")
      status(res) mustBe OK
      contentType(res) mustBe Some("application/json")
      val found = getJsValue(contentAsJson(res) \ "vatref")
      found must include(id.toString)
      //      val rec = toCount(res)
      //      rec.length mustBe > (1)

    }
  }

}
