package repository.sbrctrl

import java.time.Month.APRIL

import org.scalatest.{ FreeSpec, Matchers }
import uk.gov.ons.sbr.models.{ Period, UnitId, UnitType }

class UnitLinksPathSpec extends FreeSpec with Matchers {

  private trait Fixture {
    val TargetUnitId = UnitId("1234567890")
    val TargetPeriod = Period.fromYearMonth(2018, APRIL)
  }

  "A UnitLinks relative path" - {
    "can be built for an enterprise query" in new Fixture {
      UnitLinksPath(TargetUnitId, UnitType.Enterprise, TargetPeriod) shouldBe "v1/periods/201804/types/ENT/units/1234567890"
    }
  }
}
