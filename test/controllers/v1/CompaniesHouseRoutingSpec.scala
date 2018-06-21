package controllers.v1

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FreeSpec, Matchers }
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.matchers.HttpServerErrorStatusCodeMatcher.aServerError

/*
 * We are relying on the router to perform argument validation for us (via regex constraints).
 * This spec tests that the router is configured correctly.
 *
 * Because we are relying on the router to validate arguments, and the simple routes file configuration does not
 * compose, each and every route must be tested individually.  In addition, some of the regex are non-trivial,
 * and as the saying goes:
 *
 *     "Some people, when confronted with a problem, think 'I know, I'll use regular expressions.'
 *     Now they have two problems."
 *
 * For example, in order to fully test the period regex, we test that each and every possible month is considered
 * valid.  These are downsides of this "router based validation" approach ...
 */
class CompaniesHouseRoutingSpec extends FreeSpec with Matchers with GuiceOneAppPerSuite with ScalaFutures {

  /*
   * When valid arguments are routed to the retrieve... actions, an attempt will be made to make a call to
   * sbr-control-api.  However, we do not prime a response in this spec, as we are only concerned with routing.
   * We therefore override the HTTP timeout configuration to minimise the time this spec waits on a connection that
   * we know cannot be established.
   */
  override def fakeApplication(): Application = new GuiceApplicationBuilder().configure(Map("play.ws.timeout.connection" -> "50")).build()

  private trait Fixture {
    val ValidCompanyRefNumber = "04447084"
    val ValidPeriod = "201805"
  }

  "A request to retrieve a Companies House unit by company reference number and period" - {
    "is rejected when" - {
      "the company reference number" - {
        "has fewer than eight digits" in new Fixture {
          val CompanyRefNumberTooFewDigits = ValidCompanyRefNumber.drop(1)

          val Some(result) = route(app, FakeRequest(GET, s"/v1/periods/$ValidPeriod/crns/$CompanyRefNumberTooFewDigits"))

          status(result) shouldBe BAD_REQUEST
        }

        "has more than eight digits" in new Fixture {
          val CompanyRefNumberTooManyDigits = ValidCompanyRefNumber + "9"

          val Some(result) = route(app, FakeRequest(GET, s"/v1/periods/$ValidPeriod/crns/$CompanyRefNumberTooManyDigits"))

          status(result) shouldBe BAD_REQUEST
        }

        "is non-numeric" in new Fixture {
          val CompanyRefNumberNonNumeric = new String(Array.fill(8)('A'))

          val Some(result) = route(app, FakeRequest(GET, s"/v1/periods/$ValidPeriod/crns/$CompanyRefNumberNonNumeric"))

          status(result) shouldBe BAD_REQUEST
        }
      }

      "the Period" - {
        "has fewer than six digits" in new Fixture {
          val PeriodTooFewDigits = ValidPeriod.drop(1)

          val Some(result) = route(app, FakeRequest(GET, s"/v1/periods/$PeriodTooFewDigits/crns/$ValidCompanyRefNumber"))

          status(result) shouldBe BAD_REQUEST
        }

        "has more than six digits" in new Fixture {
          val PeriodTooManyDigits = ValidPeriod + "1"

          val Some(result) = route(app, FakeRequest(GET, s"/v1/periods/$PeriodTooManyDigits/crns/$ValidCompanyRefNumber"))

          status(result) shouldBe BAD_REQUEST
        }

        "is non-numeric" in new Fixture {
          val PeriodNonNumeric = new String(Array.fill(6)('A'))

          val Some(result) = route(app, FakeRequest(GET, s"/v1/periods/$PeriodNonNumeric/crns/$ValidCompanyRefNumber"))

          status(result) shouldBe BAD_REQUEST
        }

        "is negative" in new Fixture {
          val PeriodNegative = "-01801"

          val Some(result) = route(app, FakeRequest(GET, s"/v1/periods/$PeriodNegative/crns/$ValidCompanyRefNumber"))

          status(result) shouldBe BAD_REQUEST
        }

        "has an invalid month value" - {
          "that is too low" in new Fixture {
            val PeriodInvalidBeforeCalendarMonth = "201800"

            val Some(result) = route(app, FakeRequest(GET, s"/v1/periods/$PeriodInvalidBeforeCalendarMonth/crns/$ValidCompanyRefNumber"))

            status(result) shouldBe BAD_REQUEST
          }

          "that is too high" in new Fixture {
            val PeriodInvalidAfterCalendarMonth = "201813"

            val Some(result) = route(app, FakeRequest(GET, s"/v1/periods/$PeriodInvalidAfterCalendarMonth/crns/$ValidCompanyRefNumber"))

            status(result) shouldBe BAD_REQUEST
          }
        }
      }
    }

    /*
     * A valid request should be routed to the "retrieve" action.
     * As we are only interested in routing we have not primed a fake sbr-control-api, and so the request will fail.
     * The key for this test is that we accepted the arguments and attempted to perform a REST call - thereby
     * generating a "server error" rather than a "client error".
     */
    "is processed when valid" in new Fixture {
      val Year = "2018"
      val Months = Seq("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12")
      Months.foreach { month =>
        withClue(s"for month $month") {
          val validPeriod = Year + month
          val Some(futureResult) = route(app, FakeRequest(GET, s"/v1/periods/$validPeriod/crns/$ValidCompanyRefNumber"))

          status(futureResult) should be(aServerError)
        }
      }
    }
  }
}
