package controllers.v1

import java.time.Month.FEBRUARY

import handlers.LinkedUnitRetrievalHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FreeSpec, Matchers, OptionValues }
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.EnterpriseService
import support.sample.SampleLinkedUnit
import uk.gov.ons.sbr.models._

import scala.concurrent.Future

class EnterpriseControllerSpec extends FreeSpec with Matchers with MockFactory with OptionValues {

  private trait Fixture {
    val TargetErn = Ern("1234567890")
    val TargetPeriod = Period.fromYearMonth(2018, FEBRUARY)
    val LinkedEnterprise = SampleLinkedUnit.forEnterprise(TargetPeriod, TargetErn)
    val EnterpriseJsonRepresentation = JsObject(Seq("foo" -> JsString("bar")))

    val enterpriseService = mock[EnterpriseService]
    val retrievalHandler = mock[LinkedUnitRetrievalHandler[Result]]
    val controller = new EnterpriseController(enterpriseService, retrievalHandler)
  }

  "A request" - {
    "to retrieve an Enterprise for a period by the Enterprise reference (ERN)" - {
      "results in a response based upon the outcome of performing an enterprise lookup" in new Fixture {
        val retrievalOutcome = Right(Some(LinkedEnterprise))
        (enterpriseService.retrieve _).expects(TargetPeriod, TargetErn).returning(Future.successful(retrievalOutcome))
        (retrievalHandler.handleOutcome _).expects(retrievalOutcome).returning(Ok(EnterpriseJsonRepresentation))

        val action = controller.retrieveEnterprise(Period.asString(TargetPeriod), TargetErn.value)
        val response = action.apply(FakeRequest())

        status(response) shouldBe OK
        contentType(response).value shouldBe JSON
        contentAsJson(response) shouldBe EnterpriseJsonRepresentation
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
