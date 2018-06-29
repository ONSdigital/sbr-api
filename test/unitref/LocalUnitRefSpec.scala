package unitref

import org.scalatest.{ FreeSpec, Matchers }
import uk.gov.ons.sbr.models.UnitType.LocalUnit
import uk.gov.ons.sbr.models.{ Lurn, UnitId }

class LocalUnitRefSpec extends FreeSpec with Matchers {
  "A Local Unit reference (LURN)" - {
    "can be created from a generic unit identifier" in {
      LocalUnitRef.fromUnitId(UnitId("123456789")) shouldBe Lurn("123456789")
    }

    "can be converted to a unit Identifier and unit Type pair" in {
      LocalUnitRef.toIdTypePair(Lurn("123456789")) shouldBe UnitId("123456789") -> LocalUnit
    }

    "can be converted to a generic unit identifier" in {
      LocalUnitRef.toUnitId(Lurn("123456789")) shouldBe UnitId("123456789")
    }
  }
}
