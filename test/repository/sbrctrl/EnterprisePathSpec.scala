package repository.sbrctrl

import java.time.Month.MARCH

import org.scalatest.{ FreeSpec, Matchers }
import uk.gov.ons.sbr.models.{ Ern, Period }

class EnterprisePathSpec extends FreeSpec with Matchers {

  private trait Fixture {
    val TargetErn = Ern("1000000012")
    val TargetPeriod = Period.fromYearMonth(2018, MARCH)
  }

  "A relative path" - {
    "can be built to identify an enterprise and period" in new Fixture {
      EnterprisePath(TargetPeriod, TargetErn) shouldBe "v1/periods/201803/enterprises/1000000012"
    }
  }
}
