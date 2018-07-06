package services.finder

import java.time.Month.JUNE

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ EitherValues, FreeSpec, Matchers }
import play.api.libs.json.{ JsObject, Json }
import services.ErrorMessage
import tracing.TraceData
import uk.gov.ons.sbr.models.UnitType.{ Enterprise, LegalUnit, ReportingUnit }
import uk.gov.ons.sbr.models._
import unitref.UnitRef

import scala.concurrent.Future

class ByParentEnterpriseUnitFinderSpec extends FreeSpec with Matchers with MockFactory with ScalaFutures with EitherValues {

  private trait Fixture {
    case class FakeRef(value: String)

    val TargetPeriod = Period.fromYearMonth(2018, JUNE)
    val TargetUnitRef = FakeRef("12345678901")
    val ParentErn = Ern("9876543210")
    val TheUnitLinks = UnitLinks(
      id = UnitId(TargetUnitRef.value),
      unitType = ReportingUnit, // this is a more general test than ReportingUnits - but we need a valid UnitType
      period = TargetPeriod,
      parents = Some(Map(Enterprise -> UnitId(ParentErn.value))),
      children = None
    )
    val UnitJson = Json.parse(s"""{"some":"json"}""").as[JsObject]

    val traceData = stub[TraceData]
    val retrieveUnit = mockFunction[Period, Ern, FakeRef, TraceData, Future[Either[ErrorMessage, Option[JsObject]]]]
    val enterpriseUnitRefType = stub[UnitRef[Ern]]
    (enterpriseUnitRefType.fromUnitId _).when(UnitId(ParentErn.value)).returns(ParentErn)
    val finder = new ByParentEnterpriseUnitFinder[FakeRef](retrieveUnit, enterpriseUnitRefType)
  }

  "A ByParentEnterprise Unit finder" - {
    "retrieves a unit via the repository when the unit links contain a parent Enterprise" - {
      "returning the JSON representation when the unit is found" in new Fixture {
        retrieveUnit.expects(TargetPeriod, ParentErn, TargetUnitRef, traceData).returning(
          Future.successful(Right(Some(UnitJson)))
        )

        whenReady(finder.find(TargetPeriod, TargetUnitRef, TheUnitLinks, traceData)) { result =>
          result.right.value shouldBe Some(UnitJson)
        }
      }

      "returning nothing when the unit is not found" in new Fixture {
        retrieveUnit.expects(TargetPeriod, ParentErn, TargetUnitRef, traceData).returning(
          Future.successful(Right(None))
        )

        whenReady(finder.find(TargetPeriod, TargetUnitRef, TheUnitLinks, traceData)) { result =>
          result.right.value shouldBe empty
        }
      }

      "returning the failure message when the retrieval fails" in new Fixture {
        val failureMessage = "retrieval failed"
        retrieveUnit.expects(TargetPeriod, ParentErn, TargetUnitRef, traceData).returning(
          Future.successful(Left(failureMessage))
        )

        whenReady(finder.find(TargetPeriod, TargetUnitRef, TheUnitLinks, traceData)) { result =>
          result.left.value shouldBe failureMessage
        }
      }
    }

    "fails when the unit links do not contain a parent Enterprise" in new Fixture {
      val unitLinksMissingParentEnterprise = TheUnitLinks.copy(parents = Some(Map(LegalUnit -> UnitId("1234567890123456"))))

      whenReady(finder.find(TargetPeriod, TargetUnitRef, unitLinksMissingParentEnterprise, traceData)) { result =>
        result.left.value shouldBe s"Unit Links for unit [$TargetUnitRef] is missing the mandatory parent Enterprise."
      }
    }
  }
}
