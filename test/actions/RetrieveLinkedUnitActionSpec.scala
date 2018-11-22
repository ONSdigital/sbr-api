package actions

import java.time.Month.MAY

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{EitherValues, FreeSpec, Matchers}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContent, Request}
import services.LinkedUnitService
import tracing.TraceData
import uk.gov.ons.sbr.models.{LinkedUnit, Period, UnitId, UnitType}

import scala.concurrent.{ExecutionContext, Future}

class RetrieveLinkedUnitActionSpec extends FreeSpec with Matchers with MockFactory with ScalaFutures with EitherValues {

  private trait Fixture {
    case class FakeRef(value: String)

    val TargetPeriod = Period.fromYearMonth(2018, MAY)
    val TargetUnitRef = FakeRef("some-unit-ref")
    val ALinkedUnit = LinkedUnit(
      id = UnitId(TargetUnitRef.value),
      unitType = UnitType.LegalUnit, // this test is not about LegalUnits - but we need a valid Unit Type
      period = TargetPeriod,
      parents = None,
      children = None,
      vars = Json.parse(s"""{"unit":"data"}""").as[JsObject]
    )
    val ServiceResult = Right(Some(ALinkedUnit))

    val traceData = stub[TraceData]
    val request = stub[Request[AnyContent]]
    val service = mock[LinkedUnitService[FakeRef]]
    val retrieveLinkedUnitAction = new RetrieveLinkedUnitAction[FakeRef](service, ExecutionContext.global)
  }

  "A RetrieveLinkedUnitAction" - {
    "attempts to retrieve the linked unit and adds the outcome to the request" in new Fixture {
      (service.retrieve _).expects(TargetPeriod, TargetUnitRef, traceData).returning(Future.successful(ServiceResult))
      val action = retrieveLinkedUnitAction(TargetPeriod, TargetUnitRef)
      val tracedRequest = new TracedRequest(traceData, request)

      whenReady(action.refine(tracedRequest)) { result =>
        result.right.value.linkedUnitResult shouldBe ServiceResult
      }
    }
  }
}
