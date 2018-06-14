package controllers.v1

import java.time.Month.FEBRUARY

import actions.LinkedUnitRequest
import actions.RetrieveLinkedUnitAction.LinkedUnitRequestActionBuilderMaker
import handlers.LinkedUnitRetrievalHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FreeSpec, Matchers }
import play.api.mvc.Results.NotFound
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.ons.sbr.models.Period
import unitref.UnitRef

import scala.concurrent.Future

class LinkedUnitControllerSpec extends FreeSpec with Matchers with MockFactory {

  private trait Fixture {
    case class FakeUnitRef(value: String)

    val TargetUnitRef = FakeUnitRef("a-unit-ref")
    val TargetPeriod = Period.fromYearMonth(2018, FEBRUARY)
    val TargetUnitRetrievalResult = Right(None)

    val unitRefType = stub[UnitRef[FakeUnitRef]]
    val actionBuilderMaker = mockFunction[Period, FakeUnitRef, ActionBuilder[LinkedUnitRequest]]
    val linkedUnitRetrievalHandler = mock[LinkedUnitRetrievalHandler[Result]]

    /*
     * Invoke the 'block' supplied by the controller - this should result in the retrievalHandler being invoked.
     */
    val fakeActionBuilder = new ActionBuilder[LinkedUnitRequest] {
      override def invokeBlock[A](request: Request[A], block: LinkedUnitRequest[A] => Future[Result]): Future[Result] = {
        block(new LinkedUnitRequest[A](TargetUnitRetrievalResult, request))
      }
    }

    class FakeUnitController(retrieveLinkedUnitAction: LinkedUnitRequestActionBuilderMaker[FakeUnitRef], handleLinkedUnitRetrievalResult: LinkedUnitRetrievalHandler[Result]) extends LinkedUnitController[FakeUnitRef](unitRefType, retrieveLinkedUnitAction, handleLinkedUnitRetrievalResult) {
      // make the method public so that we can invoke it from a test
      override def retrieveLinkedUnit(periodStr: String, unitRefStr: String): Action[AnyContent] =
        super.retrieveLinkedUnit(periodStr, unitRefStr)
    }

    val controller = new FakeUnitController(actionBuilderMaker, linkedUnitRetrievalHandler)
  }

  "A request" - {
    "to retrieve a LinkedUnit for a period by the unit reference" - {
      /*
       * There is quite a bit of indirection here - but the aim is to assert that the controller invokes the
       * LinkedUnit retrieval action, forwards the result to the result handler, and returns the output of
       * the result handler unchanged.
       */
      "invokes the LinkedUnit retrieval action and handler" in new Fixture {
        (unitRefType.fromString _).when(TargetUnitRef.value).returns(TargetUnitRef)
        actionBuilderMaker.expects(TargetPeriod, TargetUnitRef).returning(fakeActionBuilder)
        (linkedUnitRetrievalHandler.apply _).expects(TargetUnitRetrievalResult).returning(NotFound)

        val action = controller.retrieveLinkedUnit(Period.asString(TargetPeriod), TargetUnitRef.value)
        val response = action.apply(FakeRequest())

        status(response) shouldBe NOT_FOUND
      }
    }
  }
}
