package utils

import com.netaporter.uri.Uri
import play.api.libs.json._
import support.TestUtils
import uk.gov.ons.sbr.models.{ CRN, ENT }
import utils.UriBuilder.createUri
import utils.Utilities._

class UtilitiesSpec extends TestUtils {

  private val TEST_JS: JsValue = Json.obj(
    "name" -> "Watership Down",
    "location" -> Json.obj("lat" -> 51.235685, "long" -> -1.309197)
  )

  "errAsJson" should {
    "create a custom err json object" in {
      val msg = "could not process request"
      val cause = "Not Traced"
      val errMsg = errAsJson(msg, cause)
      errMsg mustBe a[JsObject]
      (errMsg \ "route_with_cause").as[String] mustEqual cause
    }
  }

  "getElement" should {
    "return a Long for Option[Long]" in {
      val expected = 5784785784L
      val get = getElement(Some(expected))
      get must not be a[Option[_]]
      get mustEqual expected
    }
  }

  "unquote" should {
    "removes any unnecessary quotes" in {
      val quoted = """hello this is a\" test"""
      val parsed = unquote(quoted)
      parsed mustNot contain("\"")
    }
  }

  "getOrNull" should {
    "give JsValue" in {
      val jsName = (TEST_JS \ "name").getOrNull
      jsName mustBe a[JsValue]
      jsName mustEqual JsString("Watership Down")
    }
    "give JsNull when no valid JsValue is found" in {
      val jsName = (TEST_JS \ "location" \ "x").getOrNull
      jsName mustBe a[JsValue]
      jsName mustEqual JsNull
    }
  }

  // TODO - create history admin data
  // TODO - unit links only
  // TODO - case using the if condition
  "uriPathBuilder" should {
    "return a uri with types and units path arg set as ENT and 0000 respectively" in {
      val expected = "/localhost:8080/v0/types/ENT/units/0000"
      val uri = createUri("localhost:8080/v0", "0000", None, Some(ENT))
      uri mustBe a[Uri]
      uri.toString mustEqual expected
    }
    "return a uri for control-api route to get unit with the specified type and period param" in {
      val expected = "/localhost:8080/v0/periods/201706/types/CH/units/00011"
      val uri = createUri("localhost:8080/v0", "00011", Some("201706"), Some(CRN))
      uri mustBe a[Uri]
      uri.toString mustEqual expected
    }
    "return a uri that a request to LEU hbase rest api" in {
      val expected = "/leu-localhost:8080/v0/records/1000345"
      val uri = createUri("leu-localhost:8080/v0", "1000345", group = "ch")
      uri mustBe a[Uri]
      uri.toString mustEqual expected
    }

    "return a VAT admin data uri request with period parameter - executes first IF conditional in Builder" in {
      val expected = "/vat-localhost:8080/v0/records/9000089/periods/201901"
      val uri = createUri("/vat-localhost:8080/v0", "9000089", Some("201901"), group = "VAT")
      uri mustBe a[Uri]
      uri.toString mustEqual expected
    }

    "return a control url with id and period path in exact order - executes last IF clause in Builder" in {
      val expected = "/localhost:8080/v0/periods/201912/units/11111"
      val uri = createUri("/localhost:8080/v0", "11111", Some("201912"))
      uri mustBe a[Uri]
      uri.toString mustEqual expected
    }

    "returns a control url for a single unit param url only - no period" in {
      val expected = "/localhost:8080/v0/units/11111"
      val uri = createUri("/localhost:8080/v0", "11111")
      uri mustBe a[Uri]
      uri.toString mustEqual expected
    }

    "generate a request to a admin data api with max argument set" in {
      val max = 3
      val expected = s"/localhost:8080/v0/records/12345678/history?max=$max"
      val uri = createUri("/localhost:8080/v0", "12345678", group = "PAYE", history = Some(max))
      uri mustBe a[Uri]
      uri.toString mustEqual expected
    }
  }
}
