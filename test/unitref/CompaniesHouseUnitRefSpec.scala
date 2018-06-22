package unitref

import org.scalatest.{ FreeSpec, Matchers }
import uk.gov.ons.sbr.models.UnitType.CompaniesHouse
import uk.gov.ons.sbr.models.{ CompanyRefNumber, UnitId }

class CompaniesHouseUnitRefSpec extends FreeSpec with Matchers {

  "A Company Reference Number (CRN)" - {
    "can be created from a generic unit identifier" in {
      CompaniesHouseUnitRef.fromUnitId(UnitId("12345678")) shouldBe CompanyRefNumber("12345678")
    }

    "can be converted to a unit Identifier and unit Type pair" in {
      CompaniesHouseUnitRef.toIdTypePair(CompanyRefNumber("12345678")) shouldBe UnitId("12345678") -> CompaniesHouse
    }

    "can be converted to a generic unit identifier" in {
      CompaniesHouseUnitRef.toUnitId(CompanyRefNumber("12345678")) shouldBe UnitId("12345678")
    }
  }
}
