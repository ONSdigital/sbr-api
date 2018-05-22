package controllers.v1

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json._
import play.api.test.Helpers._
import support.TestUtils
import uk.gov.ons.sbr.models._

case class Response(id: String, period: String, unitType: String, parents: Map[String, String], vars: LocalUnit)
object Response {
  implicit val unitFormat: OFormat[Response] = Json.format[Response]
}

class LocalUnitControllerSpec extends TestUtils with BeforeAndAfterEach with GuiceOneAppPerSuite {

  private val version = "v1"
  private val port = 9001
  private val host = "localhost"
  private val wireMockServer = new WireMockServer(wireMockConfig().port(port))
  private val unitType = "LOU"

  override def beforeEach {
    wireMockServer.start()
    WireMock.configureFor(host, port)
  }

  override def afterEach {
    wireMockServer.stop()
  }

  private def mockEndpoint(path: String, status: Int, body: String = ""): Unit = stubFor(get(urlEqualTo(path))
    .willReturn(
      aResponse()
        .withStatus(status)
        .withHeader("content-type", "application/json")
        .withBody(body)
    ))

  private def createLOURecord(luref: Option[String] = None, tradingStyle: Option[String] = None): JsObject = {
    JsObject(Seq(
      "lurn" -> JsString("900000123"),
      "luref" -> JsString(luref.getOrElse(null)),
      "name" -> JsString("Company 123"),
      "tradingStyle" -> JsString(tradingStyle.getOrElse(null)),
      "sic07" -> JsString("sic07-123"),
      "employees" -> JsNumber(123),
      "enterprise" -> JsObject(Map(
        "ern" -> JsString("12345"),
        "entref" -> JsString("entref-123")
      )),
      "address" -> JsObject(Map(
        "line1" -> JsString("address-1"),
        "line2" -> JsString("address-2"),
        "line3" -> JsString("address-3"),
        "line4" -> JsString("address-4"),
        "line5" -> JsString("address-5"),
        "postcode" -> JsString("NP10 8XG")
      ))
    ))
  }

  private trait UnitLinksFixture {
    val id = "900000123"
    val entId = "12345"
    val period = "201803"

    val unitLinksJson: JsValue = JsObject(Seq(
      "id" -> JsString(id),
      "period" -> JsString(period),
      "unitType" -> JsString(unitType),
      "parents" -> JsObject(Map("ENT" -> JsString("12345")))
    ))
  }

  "/v1/periods/:date/leus/:id" should {
    "return a local unit for the given id and period (all fields present)" in new UnitLinksFixture {
      // Mock the unit links path
      val unitLinksPath = s"/v1/periods/$period/types/$unitType/units/$id"
      mockEndpoint(unitLinksPath, 200, unitLinksJson.toString)

      // Mock the path for the LOU data
      val louPath = s"/v1/enterprises/$entId/periods/$period/localunits/$id"
      val louJson: JsValue = createLOURecord(Some("company-123"), Some("A"))
      mockEndpoint(louPath, 200, louJson.toString)

      val resp = fakeRequest(s"/$version/periods/$period/lous/$id")
      val json = contentAsJson(resp)
      val louResp = json.validate[Response]

      status(resp) mustBe OK
      contentType(resp) mustBe Some("application/json")
      louResp.isInstanceOf[JsSuccess[Response]] mustBe true
      louResp.get.id mustBe id
      louResp.get.period mustBe period
      louResp.get.unitType mustBe unitType
      louResp.get.parents mustBe Map("ENT" -> "12345")
    }

    "return a local unit for the given id and period (mandatory fields present)" in new UnitLinksFixture {
      // Mock the path for the LOU data
      val louPath = s"/v1/enterprises/$entId/periods/$period/localunits/$id"
      val louJson: JsValue = createLOURecord()
      mockEndpoint(louPath, 200, louJson.toString)

      val resp = fakeRequest(s"/$version/periods/$period/lous/$id")
      val json = contentAsJson(resp)
      val louResp = json.validate[Response]

      status(resp) mustBe OK
      contentType(resp) mustBe Some("application/json")
      louResp.isInstanceOf[JsSuccess[Response]] mustBe true
      louResp.get.vars.luref mustBe None
      louResp.get.vars.tradingStyle mustBe None
      louResp.get.id mustBe id
      louResp.get.period mustBe period
      louResp.get.unitType mustBe unitType
      louResp.get.parents mustBe Map("ENT" -> "12345")
    }
  }

  "searching for a local unit which has no unit links data" should {
    "return 404 Not Found if unit links returns 404" in new UnitLinksFixture {
      val path = s"/v1/periods/201803/types/$unitType/units/$id"
      mockEndpoint(path, 404)
      val resp = fakeRequest(s"/$version/periods/$period/lous/$id")
      status(resp) mustBe NOT_FOUND
    }

    "return 500 Internal Server Error if unit links returns 500" in new UnitLinksFixture {
      val path = s"/v1/periods/201803/types/$unitType/units/$id"
      mockEndpoint(path, 500)
      val resp = fakeRequest(s"/$version/periods/$period/lous/$id")
      status(resp) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "searching for a local unit that has unit links data but no record data" should {
    "returns 404 Not Found if the data request returns 404" in new UnitLinksFixture {
      // Unit Links path
      val path = s"/v1/periods/201803/types/$unitType/units/$id"
      mockEndpoint(path, 200, unitLinksJson.toString)

      // LOU record data path
      val louPath = s"/v1/enterprises/$entId/periods/$period/localunits/$id"
      mockEndpoint(louPath, 404)

      val resp = fakeRequest(s"/$version/periods/$period/lous/$id")
      status(resp) mustBe NOT_FOUND
    }

    "returns 500 Internal Server Error if the data request returns 500" in new UnitLinksFixture {
      // Unit Links path
      val path = s"/v1/periods/201803/types/$unitType/units/$id"
      mockEndpoint(path, 200, unitLinksJson.toString)

      // LOU record data path
      val louPath = s"/v1/enterprises/$entId/periods/$period/localunits/$id"
      mockEndpoint(louPath, 500)

      val resp = fakeRequest(s"/$version/periods/$period/lous/$id")
      status(resp) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
