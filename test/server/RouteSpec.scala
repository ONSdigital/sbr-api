package scala.server

import play.api.test.Helpers._
import play.api.test._

import resource.TestUtils

/**
 * Test application routes operate
 */
class RouteSpec extends TestUtils {

  "No Route" should {
    "send 404 on a bad request" in {
      route(app, FakeRequest(GET, "/boum")).map(status) mustBe Some(NOT_FOUND)
    }
  }

  "HomeController" should {
    "render default app route" in {
      val home = fakeRequest("/")
      // redirect
      status(home) mustEqual SEE_OTHER
      val res = getValue(redirectLocation(home))
      res must include("/health")
      flash(home).get("status") mustBe Some("ok")
    }

    "display swagger documentation" in {
      val docs = fakeRequest("/docs")
      status(docs) mustEqual SEE_OTHER
      val res = getValue(redirectLocation(docs))
      res must include("/swagger-ui/index.html")
      contentAsString(docs) mustNot include("Not_FOUND")
    }
  }

  "SearchController" should {
    "return BadRequest as json error stating no expected param found" in {
      val search = fakeRequest("/v1/search?id=")
      status(search) mustBe BAD_REQUEST
      contentType(search) mustBe Some("application/json")
      val err_code: String = getJsValue(contentAsJson(search) \ "code")
      err_code mustBe s""""missing_param""""
    }

    // @todo - make badrequest route (short key size)
    "return Badrequest as json error 2with key size too small" ignore {
      val shortId = "123"
      val search = fakeRequest(s"/v1/search?id=$shortId")
      status(search) mustBe BAD_REQUEST
      contentType(search) mustBe Some("application/json")
      val err_code: String = getJsValue(contentAsJson(search) \ "code")
      err_code mustBe s""""invalid_key_size""""
    }
  }

  "VersionController" should {
    "display list of versions" in {
      val version = fakeRequest("/version")
      status(version) mustEqual OK
      contentType(version) mustBe Some("application/json")
    }
  }

  "HealthController" should {
    "display short health report as json" in {
      val health = fakeRequest("/health")
      status(health) mustEqual OK
      contentType(health) mustBe Some("application/json")
      contentAsString(health).toLowerCase must include("status: ok")
    }
  }

  // @note - in progress route
  "LastUpdateController" should {
    "display last modification listing" ignore {
      val last = fakeRequest("/latest", GET)
      status(last) mustBe NOT_FOUND
      contentType(last) mustBe Some("application/json")
    }
  }

}
