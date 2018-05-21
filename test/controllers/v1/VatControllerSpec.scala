package controllers.v1

import java.time.Month.FEBRUARY

import handlers.LinkedUnitRetrievalHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FreeSpec, Matchers, OptionValues }
import play.api.libs.json.{ JsObject, JsString }
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.VatService
import support.sample.SampleLinkedUnit
import uk.gov.ons.sbr.models._

import scala.concurrent.Future

class VatControllerSpec extends FreeSpec with Matchers with MockFactory with OptionValues {

  private trait Fixture {
    val TargetVatRef = VatRef("123412341000")
    val TargetPeriod = Period.fromYearMonth(2018, FEBRUARY)
    val LinkedVat = SampleLinkedUnit.forVat(TargetPeriod, TargetVatRef)
    val VatJsonRepresentation = JsObject(Seq("some" -> JsString("vat")))

    val vatService = mock[VatService]
    val retrievalHandler = mock[LinkedUnitRetrievalHandler[Result]]
    val controller = new VatController(vatService, retrievalHandler)
  }

  "A request" - {
    "to retrieve a VAT unit for a period by the Vat Reference" - {
      "results in a response based upon the outcome of performing a VAT lookup" in new Fixture {
        val retrievalOutcome = Right(Some(LinkedVat))
        (vatService.retrieve _).expects(TargetPeriod, TargetVatRef).returning(Future.successful(retrievalOutcome))
        (retrievalHandler.handleOutcome _).expects(retrievalOutcome).returning(Ok(VatJsonRepresentation))

        val action = controller.retrieveVat(Period.asString(TargetPeriod), TargetVatRef.value)
        val response = action.apply(FakeRequest())

        status(response) shouldBe OK
        contentType(response).value shouldBe JSON
        contentAsJson(response) shouldBe VatJsonRepresentation
      }

      /*
       * This just tests the action.
       * See VatRoutingSpec for tests that requests are routed correctly between the available actions.
       */
      "containing an invalid argument" - {
        "receives a BAD REQUEST response" in new Fixture {
          val action = controller.badRequest(Period.asString(TargetPeriod), TargetVatRef.value)
          val response = action.apply(FakeRequest())

          status(response) shouldBe BAD_REQUEST
        }
      }
    }
  }
}
