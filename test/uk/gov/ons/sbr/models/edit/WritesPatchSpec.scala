package uk.gov.ons.sbr.models.edit

import org.scalatest.{ FreeSpec, Matchers }
import play.api.libs.json.{ JsArray, JsObject, JsString, Json }
import uk.gov.ons.sbr.models.edit.OperationTypes.{ Replace, Test }

class WritesPatchSpec extends FreeSpec with Matchers {

  private trait Fixture {
    val SamplePatch: Patch = Seq(
      Operation(Test, "/parents/LEU", JsString("123456789")),
      Operation(Replace, "/parents/LEU", JsString("987654321"))
    )

    val ExpectedJsonOutput =
      """[
        |  {"op": "test", "path": "/parents/LEU", "value": "123456789"},
        |  {"op": "replace", "path": "/parents/LEU", "value": "987654321"}
        |]
      """.stripMargin

  }

  "A patch specification" - {
    "when valid" - {
      "can be successfully written to JSON" in new Fixture {
        Json.toJson(SamplePatch) shouldBe Json.parse(ExpectedJsonOutput)
      }
    }
  }
}