package unitref

import org.scalatest.{ FreeSpec, Matchers }
import uk.gov.ons.sbr.models.UnitType.ValueAddedTax
import uk.gov.ons.sbr.models.{ UnitId, VatRef }

class VatUnitRefSpec extends FreeSpec with Matchers {
  "A VAT Reference" - {
    "can be created from a String parameter" in {
      VatUnitRef.fromString("346942023239") shouldBe VatRef("346942023239")
    }

    "can be converted to a unit Identifier and unit Type pair" in {
      VatUnitRef.toIdTypePair(VatRef("346942023239")) shouldBe UnitId("346942023239") -> ValueAddedTax
    }

    "can be converted to a generic unit identifier" in {
      VatUnitRef.toUnitId(VatRef("346942023239")) shouldBe UnitId("346942023239")
    }
  }
}
