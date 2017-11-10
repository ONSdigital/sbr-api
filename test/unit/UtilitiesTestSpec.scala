package unit

import com.netaporter.uri.Uri
import play.api.libs.json._

import uk.gov.ons.sbr.models.{ CRN, ENT }

import utils.UriBuilder.uriPathBuilder
import utils.Utilities._
import resources.TestUtils

class UtilitiesTestSpec extends TestUtils {

  private val testJS: JsValue = Json.obj(
    "name" -> "Watership Down",
    "location" -> Json.obj("lat" -> 51.235685, "long" -> -1.309197)
  )

  "errAsJson" should {
    "create a custom err json object" in {
      val status = 400
      val code = "bad_request"
      val msg = "could not process request"
      val errMsg = errAsJson(status, code, msg)
      errMsg mustBe a[JsObject]
      (errMsg \ "code").as[String] mustEqual code
    }
  }

  "getElement" should {
    "return a Long for Option[Long]" in {
      val expected = 5784785784L
      val get = getElement(Some(expected))
      get must not be a[Option[Long]]
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
      val jsName = (testJS \ "name").getOrNull
      jsName mustBe a[JsValue]
      jsName mustEqual JsString("Watership Down")
    }
    "give JsNull when no valid JsValue is found" in {
      val jsName = (testJS \ "location" \ "x").getOrNull
      jsName mustBe a[JsValue]
      jsName mustEqual JsNull
    }
  }

  "uriPathBuilder" should {
    "should return a uri with types and units path arg set as ENT and 0000 respectively" in {
      val expected = "/localhost:8080/v0/types/ENT/units/0000"
      val uri = uriPathBuilder("localhost:8080/v0", "0000", None, Some(ENT))
      uri mustBe a[Uri]
      uri.toString mustEqual expected
    }
    "return a uri with period, type params and the write unit path param (companies)" in {
      val expected = "/localhost:8080/v0/periods/201706/types/CH/companies/00011"
      val uri = uriPathBuilder("localhost:8080/v0", "00011", Some("201706"), Some(CRN), "crn")
      uri mustBe a[Uri]
      uri.toString mustEqual expected
    }

  }

}
