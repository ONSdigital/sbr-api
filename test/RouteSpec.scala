package scala

import org.scalatestplus.play._
import play.api.libs.json.{ JsDefined, JsLookupResult }
import play.api.test.Helpers._
import play.api.test._

/**
 * Test application routes operate
 */
class RouteSpec extends PlaySpec with OneAppPerTest {

  private def fakeRequest(url: String, method: String = GET) =
    route(app, FakeRequest(method, url)).getOrElse(sys.error(s"Route $url does not exist"))

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
      getValue(redirectLocation(home)) must include("/health")
      flash(home).get("status") mustBe Some("ok")
    }

    "display swagger documentation" in {
      val docs = fakeRequest("/docs")
      status(docs) mustEqual SEE_OTHER
      getValue(redirectLocation(docs)) must include("/swagger-ui/index.html")
      contentAsString(docs) mustNot include("Not_FOUND")
    }
  }

  "SearchController" should {
    "return some records" in {
      val suggest = fakeRequest(s"/v1/suggest?id=")
      status(suggest) mustBe BAD_REQUEST
      contentType(suggest) mustBe Some("application/json")
      val err_code: String = getJsValue(contentAsJson(suggest) \ "code")
      err_code mustBe s""""missing parameter""""
    }
  }

  "VersionController" should {
    "display list of versions" in {
      val version = fakeRequest("/version")
      status(version) mustEqual OK
      contentType(version) mustBe Some("text/plain")
    }
  }

  "HealthController" should {
    "display short health report as json" in {
      val health = fakeRequest("/health")
      status(health) mustEqual OK
      contentType(health) mustBe Some("text/plain")
      contentAsString(health).toLowerCase must include("status: ok")
    }
  }

  // note: in progress route
  "LastUpdateController" should {
    "display last modification listing" in {
      val last = fakeRequest("/latest", GET)
      status(last) mustBe NOT_FOUND
      //      contentType(last) mustBe Some("application/json")
    }
  }

  def getValue(json: Option[String]): String = json match { case Some(x: String) => s"${x}" }

  def getJsValue(elem: JsLookupResult) = elem match { case JsDefined(y) => s"${y}" }

}
