package repository.sbrctrl

import java.time.Month.MARCH

import org.scalatest.{ FreeSpec, Matchers }
import uk.gov.ons.sbr.models.{ Ern, Period, UnitId, UnitType }

class EditAdminDataPathSpec extends FreeSpec with Matchers {

  private trait Fixture {
    val TargetId = UnitId("123456789012")
    val TargetPeriod = Period.fromYearMonth(2018, MARCH)
    val TargetUnitType = UnitType.ValueAddedTax
  }

  "A relative path" - {
    "can be built to identify a vatref, unitType and period" in new Fixture {
      EditAdminDataPath(TargetPeriod, (TargetId, TargetUnitType)) shouldBe s"v1/periods/201803/types/VAT/units/123456789012"
    }
  }
}