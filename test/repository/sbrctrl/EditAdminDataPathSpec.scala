package repository.sbrctrl

import java.time.Month.MARCH

import org.scalatest.{ FreeSpec, Matchers }
import uk.gov.ons.sbr.models.{ Period, UnitId, UnitKey, UnitType }

class EditAdminDataPathSpec extends FreeSpec with Matchers {

  private trait Fixture {
    val TargetId = UnitId("123456789012")
    val TargetPeriod = Period.fromYearMonth(2018, MARCH)
    val TargetVatUnitType = UnitType.ValueAddedTax
    val TargetPayeUnitType = UnitType.PayAsYouEarn
  }

  "A relative path" - {
    "can be built to identify a vatref, unitType and period" in new Fixture {
      EditAdminDataPath(UnitKey(TargetId, TargetVatUnitType, TargetPeriod)) shouldBe s"v1/periods/201803/types/VAT/units/123456789012"
    }

    "can be built to identify a payeref, unitType and period" in new Fixture {
      EditAdminDataPath(UnitKey(TargetId, TargetPayeUnitType, TargetPeriod)) shouldBe s"v1/periods/201803/types/PAYE/units/123456789012"
    }
  }
}