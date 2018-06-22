package repository.sbrctrl

import java.time.Month.APRIL

import org.scalatest.{ FreeSpec, Matchers }
import uk.gov.ons.sbr.models.{ Ern, Lurn, Period }

class LocalUnitPathSpec extends FreeSpec with Matchers {

  private trait Fixture {
    val TargetLurn = Lurn("987654321")
    val TargetErn = Ern("1234567890")
    val TargetPeriod = Period.fromYearMonth(2018, APRIL)
  }

  "A relative path" - {
    "can be built to identify a local unit and period" in new Fixture {
      LocalUnitPath(TargetPeriod, TargetErn, TargetLurn) shouldBe "v1/enterprises/1234567890/periods/201804/localunits/987654321"
    }
  }
}
