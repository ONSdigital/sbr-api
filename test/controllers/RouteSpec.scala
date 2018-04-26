package controllers

import play.api.test.Helpers._
import play.api.test._
import support.TestUtils

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

  // TODO - ADD new search routes => test they exist
  "SearchController" should {
    "return BadRequest as json error stating no expected param found" in {
      val search = fakeRequest("/v1/search?id=")
      status(search) mustBe BAD_REQUEST
      //      contentType(search) mustBe Some("application/json")
      contentAsString(search) must include("cannot be empty or too short")
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
    }
  }

  "LastUpdateController" should {
    "display last modification listing" in {
      val last = fakeRequest("/latest", GET)
      status(last) mustBe NOT_FOUND
    }
  }
}
