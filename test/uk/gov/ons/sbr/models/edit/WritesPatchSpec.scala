package uk.gov.ons.sbr.models.edit

import org.scalatest.{ FreeSpec, Matchers }
import play.api.libs.json.{ JsArray, JsObject, JsString, Json }
import uk.gov.ons.sbr.models.edit.OperationTypes.{ Replace, Test }

class WritesPatchSpec extends FreeSpec with Matchers {

  private trait Fixture {
    val UpdatePatch: Patch = Seq(
      TestOperation(Path("/parents/", "LEU"), JsString("123456789")),
      ReplaceOperation(Path("/parents/", "LEU"), JsString("987654321"))
    )

    val CreatePatch: Patch = Seq(
      AddOperation(Path("/children/", "123456789"), JsString("VAT"))
    )

    val RemovePatch: Patch = Seq(
      TestOperation(Path("/children/", "123456789"), JsString("VAT")),
      RemoveOperation(Path("/children/", "123456789"))
    )

    val UpdatePatchExpectedJson =
      """[
        |  {"op": "test", "path": "/parents/LEU", "value": "123456789"},
        |  {"op": "replace", "path": "/parents/LEU", "value": "987654321"}
        |]
      """.stripMargin

    val CreatePatchExpectedJson =
      """[
        |  {"op": "add", "path": "/children/123456789", "value": "VAT"}
        |]
      """.stripMargin

    val RemovePatchExpectedJson =
      """[
        |  {"op": "test", "path": "/children/123456789", "value": "VAT"},
        |  {"op": "remove", "path": "/children/123456789"}
        |]
      """.stripMargin

  }

  "A patch specification" - {
    "can be successfully written to JSON" - {
      "for a Create Patch" in new Fixture {
        Json.toJson(CreatePatch) shouldBe Json.parse(CreatePatchExpectedJson)
      }

      "for an Update Patch" in new Fixture {
        Json.toJson(UpdatePatch) shouldBe Json.parse(UpdatePatchExpectedJson)
      }

      "for a Remove Patch" in new Fixture {
        Json.toJson(RemovePatch) shouldBe Json.parse(RemovePatchExpectedJson)
      }
    }
  }
}