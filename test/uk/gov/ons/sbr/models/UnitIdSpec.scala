package uk.gov.ons.sbr.models

import org.scalatest.{ FreeSpec, Matchers }
import play.api.libs.json.{ JsError, JsNumber, JsString, JsSuccess }

class UnitIdSpec extends FreeSpec with Matchers {

  private trait Fixture {
    val ExampleUnitId = UnitId("1234567890")
  }

  "A Unit Id" - {
    "is written to Json as a simple string" in new Fixture {
      UnitId.JsonFormat.writes(ExampleUnitId) shouldBe JsString("1234567890")
    }

    "can be read from a simple Json String" in new Fixture {
      UnitId.JsonFormat.reads(JsString("1234567890")) shouldBe JsSuccess(UnitId("1234567890"))
    }

    "cannot be read from Json that is not a simple string value" in new Fixture {
      UnitId.JsonFormat.reads(JsNumber(1234567890)) shouldBe a[JsError]
    }
  }
}
