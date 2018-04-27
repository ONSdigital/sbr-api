package controllers.v1

import java.time.Month.FEBRUARY

import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FreeSpec, Matchers, OptionValues }
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.EnterpriseService
import support.sample.SampleEnterprise
import uk.gov.ons.sbr.models._

import scala.concurrent.Future

class EnterpriseControllerSpec extends FreeSpec with Matchers with MockFactory with OptionValues {

  private trait Fixture {
    val TargetErn = Ern("1234567890")
    val TargetPeriod = Period.fromYearMonth(2018, FEBRUARY)
    val LinkedEnterprise = linkedEnterpriseFor(TargetPeriod, TargetErn)
    val EnterpriseJsonRepresentation = JsObject(Seq("foo" -> JsString("bar")))

    val enterpriseService = mock[EnterpriseService]
    val writesLinkedUnit = mock[Writes[LinkedUnit]]
    val controller = new EnterpriseController(enterpriseService, writesLinkedUnit)

    private def linkedEnterpriseFor(period: Period, ern: Ern): LinkedUnit =
      LinkedUnit(
        UnitId(ern.value),
        UnitType.Enterprise,
        period,
        children = Some(Map(UnitId("987654321") -> UnitType.LocalUnit)),
        vars = SampleEnterprise.asJson(TargetErn)
      )
  }

  "A request" - {
    "to retrieve an Enterprise for a period by the Enterprise reference (ERN)" - {
      "returns a JSON representation of the enterprise and its relations when it is found" in new Fixture {
        (enterpriseService.retrieve _).expects(TargetPeriod, TargetErn).returning(Future.successful(
          Right(Some(LinkedEnterprise))
        ))
        (writesLinkedUnit.writes _).expects(LinkedEnterprise).returning(EnterpriseJsonRepresentation)

        val action = controller.retrieveEnterprise(Period.asString(TargetPeriod), TargetErn.value)
        val response = action.apply(FakeRequest())

        status(response) shouldBe OK
        contentType(response).value shouldBe JSON
        contentAsJson(response) shouldBe EnterpriseJsonRepresentation
      }

      "returns NOT_FOUND when either the enterprise or its relations cannot be found" in new Fixture {
        (enterpriseService.retrieve _).expects(TargetPeriod, TargetErn).returning(Future.successful(Right(None)))

        val action = controller.retrieveEnterprise(Period.asString(TargetPeriod), TargetErn.value)
        val response = action.apply(FakeRequest())

        status(response) shouldBe NOT_FOUND
      }

      "returns GATEWAY_TIMEOUT when the retrieval time exceeds the configured time out" in new Fixture {
        (enterpriseService.retrieve _).expects(TargetPeriod, TargetErn).returning(
          Future.successful(Left("Timeout"))
        )

        val action = controller.retrieveEnterprise(Period.asString(TargetPeriod), TargetErn.value)
        val response = action.apply(FakeRequest())

        status(response) shouldBe GATEWAY_TIMEOUT
      }

      "returns INTERNAL_SERVER_ERROR when the retrieval fails" in new Fixture {
        (enterpriseService.retrieve _).expects(TargetPeriod, TargetErn).returning(
          Future.successful(Left("Retrieval failed"))
        )

        val action = controller.retrieveEnterprise(Period.asString(TargetPeriod), TargetErn.value)
        val response = action.apply(FakeRequest())

        status(response) shouldBe INTERNAL_SERVER_ERROR
      }

      /*
       * This just tests the action.
       * See EnterpriseRoutingSpec for tests that requests are routed correctly between the available actions.
       */
      "containing an invalid argument" - {
        "receives a BAD REQUEST response" in new Fixture {
          val action = controller.badRequest(Period.asString(TargetPeriod), TargetErn.value)
          val response = action.apply(FakeRequest())

          status(response) shouldBe BAD_REQUEST
        }
      }
    }
  }
}
