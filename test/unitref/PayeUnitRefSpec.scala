package unitref

import org.scalatest.{ FreeSpec, Matchers }
import uk.gov.ons.sbr.models.UnitType.PayAsYouEarn
import uk.gov.ons.sbr.models.{ PayeRef, UnitId }

class PayeUnitRefSpec extends FreeSpec with Matchers {
  "A PAYE Reference" - {
    "can be created from a String parameter" in {
      PayeUnitRef.fromString("4124642") shouldBe PayeRef("4124642")
    }

    "can be converted to a unit Identifier and unit Type pair" in {
      PayeUnitRef.toIdTypePair(PayeRef("4124642")) shouldBe UnitId("4124642") -> PayAsYouEarn
    }

    "can be converted to a generic unit identifier" in {
      PayeUnitRef.toUnitId(PayeRef("4124642")) shouldBe UnitId("4124642")
    }
  }
}
