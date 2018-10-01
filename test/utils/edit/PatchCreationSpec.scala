package utils.edit

import org.scalatest.{ FreeSpec, Matchers }
import play.api.libs.json.JsString
import uk.gov.ons.sbr.models.UnitType.{ LegalUnit, ValueAddedTax }
import uk.gov.ons.sbr.models.edit._
import uk.gov.ons.sbr.models._

class PatchCreationSpec extends FreeSpec with Matchers {

  private val TargetFromLEU = UnitId("123456789")
  private val TargetToLEU = UnitId("987654321")

  private val TargetUpdateFromIdAndType = IdAndType(TargetFromLEU, ValueAddedTax)
  private val TargetUpdateToIdAndType = IdAndType(TargetToLEU, ValueAddedTax)
  private val TargetCreateIdAndType = IdAndType(TargetToLEU, LegalUnit)

  "PatchCreation object" - {
    "creates an update parent patch" in {
      PatchCreation.buildUpdateParentPatch(TargetUpdateFromIdAndType, TargetUpdateToIdAndType) shouldBe Seq(
        TestOperation(Path("/parents/", "VAT"), JsString(TargetFromLEU.value)),
        ReplaceOperation(Path("/parents/", "VAT"), JsString(TargetToLEU.value))
      )
    }

    "creates a create child patch" in {
      PatchCreation.buildCreateChildPatch(TargetCreateIdAndType) shouldBe Seq(
        AddOperation(Path("/children/", TargetToLEU.value), JsString("LEU"))
      )
    }

    "creates a delete child patch" in {
      PatchCreation.buildDeleteLeuChildPatch(TargetUpdateFromIdAndType) shouldBe Seq(
        TestOperation(Path("/children/", TargetFromLEU.value), JsString("VAT")),
        RemoveOperation(Path("/children/", TargetFromLEU.value))
      )
    }
  }
}