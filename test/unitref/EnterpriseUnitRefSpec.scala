package unitref

import org.scalatest.{ FreeSpec, Matchers }
import uk.gov.ons.sbr.models.UnitType.Enterprise
import uk.gov.ons.sbr.models.{ Ern, UnitId }

class EnterpriseUnitRefSpec extends FreeSpec with Matchers {
  "An Enterprise reference (ERN)" - {
    "can be created from a generic unit identifier" in {
      EnterpriseUnitRef.fromUnitId(UnitId("1234567890")) shouldBe Ern("1234567890")
    }

    "can be converted to a unit Identifier and unit Type pair" in {
      EnterpriseUnitRef.toIdTypePair(Ern("1234567890")) shouldBe UnitId("1234567890") -> Enterprise
    }

    "can be converted to a generic unit identifier" in {
      EnterpriseUnitRef.toUnitId(Ern("1234567890")) shouldBe UnitId("1234567890")
    }
  }
}
