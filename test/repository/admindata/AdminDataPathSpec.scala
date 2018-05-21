package repository.admindata

import java.time.Month.MARCH

import org.scalatest.{ FreeSpec, Matchers }
import uk.gov.ons.sbr.models.{ Period, UnitId }

class AdminDataPathSpec extends FreeSpec with Matchers {

  private trait Fixture {
    val TargetUnitId = UnitId("397585634298")
    val TargetPeriod = Period.fromYearMonth(2018, MARCH)
  }

  "An Admin Data relative path" - {
    "can be built to identify a unit and period" in new Fixture {
      AdminDataPath(TargetUnitId, TargetPeriod) shouldBe "v1/records/397585634298/periods/201803"
    }
  }
}
