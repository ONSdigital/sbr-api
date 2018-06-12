package uk.gov.ons.sbr.models

import org.scalatest.{ FreeSpec, Matchers }

class VatRefSpec extends FreeSpec with Matchers {
  "A VAT Reference" - {
    "can be converted to a unit Identifier and unit Type pair" in {
      VatRef.toIdTypePair(VatRef("346942023239")) shouldBe UnitId("346942023239") -> UnitType.ValueAddedTax
    }

    "can be converted to a generic unit identifier" in {
      VatRef.asUnitId(VatRef("346942023239")) shouldBe UnitId("346942023239")
    }
  }
}
