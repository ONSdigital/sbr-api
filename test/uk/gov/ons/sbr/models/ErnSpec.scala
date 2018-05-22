package uk.gov.ons.sbr.models

import org.scalatest.{ FreeSpec, Matchers }

class ErnSpec extends FreeSpec with Matchers {
  "An Enterprise reference (ERN)" - {
    "can be converted to a unit Identifier and unit Type pair" in {
      Ern.toIdTypePair(Ern("1234567890")) shouldBe UnitId("1234567890") -> UnitType.Enterprise
    }
  }
}
