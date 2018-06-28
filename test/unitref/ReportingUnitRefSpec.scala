package unitref

import org.scalatest.{ FreeSpec, Matchers }
import uk.gov.ons.sbr.models.UnitType.ReportingUnit
import uk.gov.ons.sbr.models.{ Rurn, UnitId }

class ReportingUnitRefSpec extends FreeSpec with Matchers {
  "A Reporting Unit reference (RURN)" - {
    "can be created from a generic unit identifier" in {
      ReportingUnitRef.fromUnitId(UnitId("12345678901")) shouldBe Rurn("12345678901")
    }

    "can be converted to a unit Identifier and unit Type pair" in {
      ReportingUnitRef.toIdTypePair(Rurn("12345678901")) shouldBe UnitId("12345678901") -> ReportingUnit
    }

    "can be converted to a generic unit identifier" in {
      ReportingUnitRef.toUnitId(Rurn("12345678901")) shouldBe UnitId("12345678901")
    }
  }
}
