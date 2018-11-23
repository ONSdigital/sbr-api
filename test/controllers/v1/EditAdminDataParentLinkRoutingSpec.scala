package controllers.v1

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FreeSpec, Matchers }
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.mvc.Http.HeaderNames.CONTENT_TYPE
import support.matchers.HttpServerErrorStatusCodeMatcher.aServerError
import uk.gov.ons.sbr.models.UnitType.LegalUnit
import uk.gov.ons.sbr.models.{ EditParentLink, IdAndType, Parent, UnitId }

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
class EditAdminDataParentLinkRoutingSpec extends FreeSpec with Matchers with GuiceOneAppPerSuite with ScalaFutures {

  /*
   * When valid arguments are routed to the retrieve... actions, an attempt will be made to make a call to
   * sbr-control-api.  However, we do not prime a response in this spec, as we are only concerned with routing.
   * We therefore override the HTTP timeout configuration to minimise the time this spec waits on a connection that
   * we know cannot be established.
   */

  override def fakeApplication(): Application = new GuiceApplicationBuilder().configure(Map("play.ws.timeout.connection" -> "50ms")).build()

  private trait Fixture {
    val ValidVatRef = "987628475012"
    val ValidPayeRef = "123456"
    val ValidPayeRefMaxLength = "123456789012"
    val ValidPayeRefMinLength = "1234"
    val ValidPayeRefAlphaNumeric = "abCD1234"
    val ValidPeriod = "201803"

    val EditParentLinkPostBody = EditParentLink(
      Parent(
        IdAndType(UnitId("1234567890"), LegalUnit),
        IdAndType(UnitId("9876543210"), LegalUnit)
      ),
      Map("username" -> "abcdx")
    )
  }

  "A request to edit a VAT parent LEU unit link by VAT reference and period" - {
    "is rejected when" - {
      "the VAT reference" - {
        "has fewer than twelve digits" in new Fixture {
          val VatRefTooFewDigits = ValidVatRef.drop(1)

          val Some(result) = route(app, FakeRequest(POST, s"/v1/periods/$ValidPeriod/edit/vats/$VatRefTooFewDigits")
            .withHeaders(CONTENT_TYPE -> JSON)
            .withJsonBody(Json.toJson(EditParentLinkPostBody)))

          status(result) shouldBe BAD_REQUEST
        }

        "has more than twelve digits" in new Fixture {
          val VatRefTooManyDigits = ValidVatRef + "9"

          val Some(result) = route(app, FakeRequest(POST, s"/v1/periods/$ValidPeriod/edit/vats/$VatRefTooManyDigits")
            .withHeaders(CONTENT_TYPE -> JSON)
            .withJsonBody(Json.toJson(EditParentLinkPostBody)))

          status(result) shouldBe BAD_REQUEST
        }

        "is non-numeric" in new Fixture {
          val VatRefNonNumeric = new String(Array.fill(12)('A'))

          val Some(result) = route(app, FakeRequest(POST, s"/v1/periods/$ValidPeriod/edit/vats/$VatRefNonNumeric")
            .withHeaders(CONTENT_TYPE -> JSON)
            .withJsonBody(Json.toJson(EditParentLinkPostBody)))

          status(result) shouldBe BAD_REQUEST
        }
      }

      "the Period" - {
        "has fewer than six digits" in new Fixture {
          val PeriodTooFewDigits = ValidPeriod.drop(1)

          val Some(result) = route(app, FakeRequest(POST, s"/v1/periods/$PeriodTooFewDigits/edit/vats/$ValidVatRef")
            .withHeaders(CONTENT_TYPE -> JSON)
            .withJsonBody(Json.toJson(EditParentLinkPostBody)))

          status(result) shouldBe BAD_REQUEST
        }

        "has more than six digits" in new Fixture {
          val PeriodTooManyDigits = ValidPeriod + "1"

          val Some(result) = route(app, FakeRequest(POST, s"/v1/periods/$PeriodTooManyDigits/edit/vats/$ValidVatRef")
            .withHeaders(CONTENT_TYPE -> JSON)
            .withJsonBody(Json.toJson(EditParentLinkPostBody)))

          status(result) shouldBe BAD_REQUEST
        }

        "is non-numeric" in new Fixture {
          val PeriodNonNumeric = new String(Array.fill(6)('A'))

          val Some(result) = route(app, FakeRequest(POST, s"/v1/periods/$PeriodNonNumeric/edit/vats/$ValidVatRef")
            .withHeaders(CONTENT_TYPE -> JSON)
            .withJsonBody(Json.toJson(EditParentLinkPostBody)))

          status(result) shouldBe BAD_REQUEST
        }

        "is negative" in new Fixture {
          val PeriodNegative = "-01801"

          val Some(result) = route(app, FakeRequest(POST, s"/v1/periods/$PeriodNegative/edit/vats/$ValidVatRef")
            .withHeaders(CONTENT_TYPE -> JSON)
            .withJsonBody(Json.toJson(EditParentLinkPostBody)))

          status(result) shouldBe BAD_REQUEST
        }

        "has an invalid month value" - {
          "that is too low" in new Fixture {
            val PeriodInvalidBeforeCalendarMonth = "201800"

            val Some(result) = route(app, FakeRequest(POST, s"/v1/periods/$PeriodInvalidBeforeCalendarMonth/edit/vats/$ValidVatRef")
              .withHeaders(CONTENT_TYPE -> JSON)
              .withJsonBody(Json.toJson(EditParentLinkPostBody)))

            status(result) shouldBe BAD_REQUEST
          }

          "that is too high" in new Fixture {
            val PeriodInvalidAfterCalendarMonth = "201813"

            val Some(result) = route(app, FakeRequest(POST, s"/v1/periods/$PeriodInvalidAfterCalendarMonth/edit/vats/$ValidVatRef")
              .withHeaders(CONTENT_TYPE -> JSON)
              .withJsonBody(Json.toJson(EditParentLinkPostBody)))

            status(result) shouldBe BAD_REQUEST
          }
        }
      }
    }

    "A request to edit a PAYE parent LEU unit link by PAYE reference and period" - {
      "is rejected when" - {
        "the PAYE reference" - {
          "has fewer than four digits" in new Fixture {
            val PayeRefTooFewDigits = ValidPayeRefMinLength.drop(1)

            val Some(result) = route(app, FakeRequest(POST, s"/v1/periods/$ValidPeriod/edit/payes/$PayeRefTooFewDigits")
              .withHeaders(CONTENT_TYPE -> JSON)
              .withJsonBody(Json.toJson(EditParentLinkPostBody)))

            status(result) shouldBe BAD_REQUEST
          }

          "has more than twelve digits" in new Fixture {
            val PayeRefTooManyDigits = ValidPayeRefMaxLength + "9"

            val Some(result) = route(app, FakeRequest(POST, s"/v1/periods/$ValidPeriod/edit/payes/$PayeRefTooManyDigits")
              .withHeaders(CONTENT_TYPE -> JSON)
              .withJsonBody(Json.toJson(EditParentLinkPostBody)))

            status(result) shouldBe BAD_REQUEST
          }

          "is non-alpha-numeric" in new Fixture {
            val PayeRefNonNumeric = new String(Array.fill(12)('['))

            val Some(result) = route(app, FakeRequest(POST, s"/v1/periods/$ValidPeriod/edit/payes/$PayeRefNonNumeric")
              .withHeaders(CONTENT_TYPE -> JSON)
              .withJsonBody(Json.toJson(EditParentLinkPostBody)))

            status(result) shouldBe BAD_REQUEST
          }
        }

        "the Period" - {
          "has fewer than six digits" in new Fixture {
            val PeriodTooFewDigits = ValidPeriod.drop(1)

            val Some(result) = route(app, FakeRequest(POST, s"/v1/periods/$PeriodTooFewDigits/edit/payes/$ValidPayeRef")
              .withHeaders(CONTENT_TYPE -> JSON)
              .withJsonBody(Json.toJson(EditParentLinkPostBody)))

            status(result) shouldBe BAD_REQUEST
          }

          "has more than six digits" in new Fixture {
            val PeriodTooManyDigits = ValidPeriod + "1"

            val Some(result) = route(app, FakeRequest(POST, s"/v1/periods/$PeriodTooManyDigits/edit/payes/$ValidPayeRef")
              .withHeaders(CONTENT_TYPE -> JSON)
              .withJsonBody(Json.toJson(EditParentLinkPostBody)))

            status(result) shouldBe BAD_REQUEST
          }

          "is non-numeric" in new Fixture {
            val PeriodNonNumeric = new String(Array.fill(6)('A'))

            val Some(result) = route(app, FakeRequest(POST, s"/v1/periods/$PeriodNonNumeric/edit/payes/$ValidPayeRef")
              .withHeaders(CONTENT_TYPE -> JSON)
              .withJsonBody(Json.toJson(EditParentLinkPostBody)))

            status(result) shouldBe BAD_REQUEST
          }

          "is negative" in new Fixture {
            val PeriodNegative = "-01801"

            val Some(result) = route(app, FakeRequest(POST, s"/v1/periods/$PeriodNegative/edit/payes/$ValidPayeRef")
              .withHeaders(CONTENT_TYPE -> JSON)
              .withJsonBody(Json.toJson(EditParentLinkPostBody)))

            status(result) shouldBe BAD_REQUEST
          }

          "has an invalid month value" - {
            "that is too low" in new Fixture {
              val PeriodInvalidBeforeCalendarMonth = "201800"

              val Some(result) = route(app, FakeRequest(POST, s"/v1/periods/$PeriodInvalidBeforeCalendarMonth/edit/payes/$ValidPayeRef")
                .withHeaders(CONTENT_TYPE -> JSON)
                .withJsonBody(Json.toJson(EditParentLinkPostBody)))

              status(result) shouldBe BAD_REQUEST
            }

            "that is too high" in new Fixture {
              val PeriodInvalidAfterCalendarMonth = "201813"

              val Some(result) = route(app, FakeRequest(POST, s"/v1/periods/$PeriodInvalidAfterCalendarMonth/edit/payes/$ValidPayeRef")
                .withHeaders(CONTENT_TYPE -> JSON)
                .withJsonBody(Json.toJson(EditParentLinkPostBody)))

              status(result) shouldBe BAD_REQUEST
            }
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
          val Some(futureResult) = route(app, FakeRequest(POST, s"/v1/periods/$validPeriod/edit/vats/$ValidVatRef")
            .withHeaders(CONTENT_TYPE -> JSON)
            .withBody(Json.toJson(EditParentLinkPostBody)))

          status(futureResult) should be(aServerError)
        }
      }
    }

    "supporting the minimum PAYE reference length" in new Fixture {
      val Some(futureResult) = route(app, FakeRequest(POST, s"/v1/periods/201801/edit/payes/$ValidPayeRefMinLength")
        .withHeaders(CONTENT_TYPE -> JSON)
        .withJsonBody(Json.toJson(EditParentLinkPostBody)))

      status(futureResult) should be(aServerError)
    }

    "supporting the maximum PAYE reference length" in new Fixture {
      val Some(futureResult) = route(app, FakeRequest(POST, s"/v1/periods/201801/edit/payes/$ValidPayeRefMaxLength")
        .withHeaders(CONTENT_TYPE -> JSON)
        .withJsonBody(Json.toJson(EditParentLinkPostBody)))

      status(futureResult) should be(aServerError)
    }

    "supporting an alphanumeric PAYE reference" in new Fixture {
      val Some(futureResult) = route(app, FakeRequest(POST, s"/v1/periods/201801/edit/payes/$ValidPayeRefAlphaNumeric")
        .withHeaders(CONTENT_TYPE -> JSON)
        .withJsonBody(Json.toJson(EditParentLinkPostBody)))

      status(futureResult) should be(aServerError)
    }
  }
}