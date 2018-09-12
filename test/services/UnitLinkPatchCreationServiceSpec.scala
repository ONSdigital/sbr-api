package services

import org.scalatest.{ FreeSpec, Matchers }
import play.api.libs.json.JsString
import uk.gov.ons.sbr.models._
import uk.gov.ons.sbr.models.edit.{ Operation, OperationTypes }

class UnitLinkPatchCreationServiceSpec extends FreeSpec with Matchers {

  private trait Fixture {
    val TargetPath = "/parents/LEU"
    val TargetFromLEU = "123456789"
    val TargetToLEU = "987654321"
    val editParentLink = EditParentLink(
      Parent(
        IdAndType(UnitId(TargetFromLEU), UnitType.LegalUnit),
        IdAndType(UnitId(TargetToLEU), UnitType.LegalUnit)
      ),
      Map("username" -> "abcd")
    )
    val converter = new UnitLinkPatchCreationService
  }

  "PostJsonModelToPatch" - {
    "converts an EditParentLink to a Patch" in new Fixture {
      converter.createPatch(editParentLink) shouldBe Right(Seq(
        Operation(OperationTypes.Test, TargetPath, JsString(TargetFromLEU)),
        Operation(OperationTypes.Replace, TargetPath, JsString(TargetToLEU))
      ))
    }
  }
}