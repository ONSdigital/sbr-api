package utils.edit

import org.scalatest.{ FreeSpec, Matchers }
import play.api.libs.json.JsString
import uk.gov.ons.sbr.models.UnitType.{ LegalUnit, ValueAddedTax }
import uk.gov.ons.sbr.models.edit.{ Operation, Path }
import uk.gov.ons.sbr.models.edit.OperationTypes.{ Add, Replace, Test }
import uk.gov.ons.sbr.models.{ UnitId, _ }

class PatchCreationSpec extends FreeSpec with Matchers {

  private val TargetFromLEU = UnitId("123456789")
  private val TargetToLEU = UnitId("987654321")

  private val TargetFromIdAndType = IdAndType(TargetFromLEU, ValueAddedTax)
  private val TargetToIdAndType = IdAndType(TargetToLEU, ValueAddedTax)

  "PatchCreation object" - {
    "creates an update parent patch" in {
      PatchCreation.buildUpdateParentPatch(TargetFromIdAndType, TargetToIdAndType) shouldBe Seq(
        Operation(Test, Path("/parents/", "VAT"), JsString(TargetFromLEU.value)),
        Operation(Replace, Path("/parents/", "VAT"), JsString(TargetToLEU.value))
      )
    }

    "creates a create child patch" in {
      PatchCreation.buildCreateChildPatch(TargetToLEU, LegalUnit) shouldBe Seq(
        Operation(Add, Path("/children/", TargetToLEU.value), JsString("LEU"))
      )
    }
  }
}