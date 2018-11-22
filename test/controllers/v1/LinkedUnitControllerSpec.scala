package controllers.v1

import java.time.Month.FEBRUARY

import actions.RetrieveLinkedUnitAction.LinkedUnitTracedRequestActionFunctionMaker
import actions.{RetrieveLinkedUnitAction, TracedRequest, WithTracingAction}
import handlers.LinkedUnitRetrievalHandler
import jp.co.bizreach.trace.ZipkinTraceServiceLike
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpec, Matchers}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.Results.NotFound
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, StubPlayBodyParsersFactory}
import services.LinkedUnitService
import support.tracing.FakeTracing
import tracing.TraceData
import uk.gov.ons.sbr.models.{Period, UnitId}
import unitref.UnitRef

import scala.concurrent.{ExecutionContext, Future}

class LinkedUnitControllerSpec extends FreeSpec with Matchers with MockFactory with FakeTracing with GuiceOneAppPerSuite {

  private trait Fixture extends StubPlayBodyParsersFactory {
    case class FakeUnitRef(value: String)
    class FakeUnitController(
        unitRefType: UnitRef[FakeUnitRef],
        withTracingAction: ActionBuilder[TracedRequest, AnyContent],
        retrieveLinkedUnitAction: LinkedUnitTracedRequestActionFunctionMaker[FakeUnitRef],
        handleLinkedUnitRetrievalResult: LinkedUnitRetrievalHandler[Result]
    ) extends LinkedUnitController[FakeUnitRef](unitRefType, withTracingAction, retrieveLinkedUnitAction, handleLinkedUnitRetrievalResult) {

      // make the method public so that we can invoke it from a test
      override def retrieveLinkedUnit(periodStr: String, unitRefStr: String): Action[AnyContent] =
        super.retrieveLinkedUnit(periodStr, unitRefStr)
    }

    val TargetUnitRef = FakeUnitRef("a-unit-ref")
    val TargetPeriod = Period.fromYearMonth(2018, FEBRUARY)
    val ServiceResult = Right(None)
    val TraceIdHigh = 0x16723998292fc385L
    val TraceIdLow = 0x45eb009b053d4091L
    val ParentSpanId = 0x5d921a12b2a59500L
    val SpanId = 0x88de528b04206226L

    val unitRefType = stub[UnitRef[FakeUnitRef]]
    val linkedUnitService = mock[LinkedUnitService[FakeUnitRef]]
    val linkedUnitRetrievalHandler = mock[LinkedUnitRetrievalHandler[Result]]
    val tracerService: ZipkinTraceServiceLike = new StubTraceService(fakeSpan(TraceIdHigh, TraceIdLow, ParentSpanId, SpanId))

    /*
     * We use the real implementations of the Actions here.  Trying to mock/stub these got complicated very quickly.
     */
    implicit val materializer = app.materializer
    val controller = new FakeUnitController(
      unitRefType,
      new WithTracingAction(stubPlayBodyParsers.default, tracerService)(ExecutionContext.global),
      new RetrieveLinkedUnitAction[FakeUnitRef](linkedUnitService, ExecutionContext.global),
      linkedUnitRetrievalHandler
    )
  }

  "A request" - {
    "to retrieve a LinkedUnit for a period by the unit reference" - {
      "is processed by utilising the common actions and request handler" in new Fixture {
        (unitRefType.fromUnitId _).when(UnitId(TargetUnitRef.value)).returns(TargetUnitRef)
        (linkedUnitService.retrieve _).expects(where { (period: Period, unitRef: FakeUnitRef, traceData: TraceData) =>
          period == TargetPeriod &&
            unitRef == TargetUnitRef &&
            aTraceContext(
              withTraceIdHigh = TraceIdHigh,
              withTraceIdLow = TraceIdLow,
              withParentSpanId = ParentSpanId,
              withSpanId = SpanId
            )(traceData.asSpan.context())
        }).returning(Future.successful(ServiceResult))
        (linkedUnitRetrievalHandler.apply _).expects(ServiceResult).returning(NotFound)

        val action = controller.retrieveLinkedUnit(Period.asString(TargetPeriod), TargetUnitRef.value)
        val response = action.apply(FakeRequest())

        status(response) shouldBe NOT_FOUND
      }
    }
  }
}
