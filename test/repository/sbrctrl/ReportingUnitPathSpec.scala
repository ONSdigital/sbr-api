package repository.sbrctrl

import java.time.Month.APRIL

import org.scalatest.{ FreeSpec, Matchers }
import uk.gov.ons.sbr.models.{ Ern, Period, Rurn }

class ReportingUnitPathSpec extends FreeSpec with Matchers {

  private trait Fixture {
    val TargetRurn = Rurn("19876543210")
    val TargetErn = Ern("1234567890")
    val TargetPeriod = Period.fromYearMonth(2018, APRIL)
  }

  "A relative path" - {
    "can be built to identify a reporting unit and period" in new Fixture {
      ReportingUnitPath(TargetPeriod, TargetErn, TargetRurn) shouldBe "v1/enterprises/1234567890/periods/201804/reportingunits/19876543210"
    }
  }
}
