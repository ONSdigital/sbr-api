package services

import java.time.Month.FEBRUARY

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{EitherValues, FreeSpec, Matchers}
import play.api.libs.json.{JsObject, Json}
import repository.UnitLinksRepository
import services.finder.UnitFinder
import tracing.TraceData
import uk.gov.ons.sbr.models.UnitType.Enterprise
import uk.gov.ons.sbr.models.{UnitType, _}
import unitref.UnitRef

import scala.concurrent.{ExecutionContext, Future}

class RestLinkedUnitServiceSpec extends FreeSpec with Matchers with MockFactory with ScalaFutures with EitherValues {

  private trait Fixture {
    case class FakeRef(value: String)

    val TargetUnitRef = FakeRef("1234")
    val TargetPeriod = Period.fromYearMonth(2018, FEBRUARY)
    val TargetUnitId = UnitId(TargetUnitRef.value)
    val TargetUnitType = Enterprise // this test is not focused on enterprises - we just need a valid UnitType
    val TargetUnitLinks = UnitLinks(
      TargetUnitId,
      TargetUnitType,
      TargetPeriod,
      parents = Some(Map(Enterprise -> UnitId("1234567890"))),
      children = Some(Map(UnitId("987654321") -> UnitType.LocalUnit))
    )
    val TargetUnitJson = Json.parse(s"""{"fake":"json"}""").as[JsObject]
    val traceData = stub[TraceData]

    val unitRefType = stub[UnitRef[FakeRef]]
    (unitRefType.toIdTypePair _).when(TargetUnitRef).returns(TargetUnitId -> TargetUnitType)

    val unitLinksRepository = mock[UnitLinksRepository]
    val unitFinder = mock[UnitFinder[FakeRef]]
    val service = new RestLinkedUnitService[FakeRef](unitRefType, unitLinksRepository, unitFinder)(ExecutionContext.global)
  }

  "A Rest LinkedUnitService" - {
    "assembles a unit with its associated links" - {
      "when both the unit link and unit entries are found for the target unit reference and period" in new Fixture {
        (unitLinksRepository.retrieveUnitLinks _).expects(TargetUnitId, TargetUnitType, TargetPeriod, traceData).returning(
          Future.successful(Right(Some(TargetUnitLinks)))
        )
        (unitFinder.find _).expects(TargetPeriod, TargetUnitRef, TargetUnitLinks, traceData).returning(
          Future.successful(Right(Some(TargetUnitJson)))
        )

        whenReady(service.retrieve(TargetPeriod, TargetUnitRef, traceData)) { result =>
          result.right.value shouldBe Some(LinkedUnit(
            TargetUnitId,
            TargetUnitType,
            TargetPeriod,
            TargetUnitLinks.parents,
            TargetUnitLinks.children,
            TargetUnitJson
          ))
        }
      }
    }

    "returns nothing" - {
      "when the links of the target unit cannot be found" in new Fixture {
        (unitLinksRepository.retrieveUnitLinks _).expects(TargetUnitId, TargetUnitType, TargetPeriod, traceData).returning(
          Future.successful(Right(None))
        )

        whenReady(service.retrieve(TargetPeriod, TargetUnitRef, traceData)) { result =>
          result.right.value shouldBe empty
        }
      }

      "when the unit itself cannot be found" in new Fixture {
        (unitLinksRepository.retrieveUnitLinks _).expects(TargetUnitId, TargetUnitType, TargetPeriod, traceData).returning(
          Future.successful(Right(Some(TargetUnitLinks)))
        )
        (unitFinder.find _).expects(TargetPeriod, TargetUnitRef, TargetUnitLinks, traceData).returning(
          Future.successful(Right(None))
        )

        whenReady(service.retrieve(TargetPeriod, TargetUnitRef, traceData)) { result =>
          result.right.value shouldBe empty
        }
      }
    }

    "returns an error message" - {
      "when retrieval of unit links fails" in new Fixture {
        val failureMessage = "unitLinks retrieval failure"
        (unitLinksRepository.retrieveUnitLinks _).expects(TargetUnitId, TargetUnitType, TargetPeriod, traceData).returning(
          Future.successful(Left(failureMessage))
        )

        whenReady(service.retrieve(TargetPeriod, TargetUnitRef, traceData)) { result =>
          result.left.value shouldBe failureMessage
        }
      }

      "when retrieval of the unit itself fails" in new Fixture {
        val failureMessage = "unit retrieval failure"
        (unitLinksRepository.retrieveUnitLinks _).expects(TargetUnitId, TargetUnitType, TargetPeriod, traceData).returning(
          Future.successful(Right(Some(TargetUnitLinks)))
        )
        (unitFinder.find _).expects(TargetPeriod, TargetUnitRef, TargetUnitLinks, traceData).returning(
          Future.successful(Left(failureMessage))
        )

        whenReady(service.retrieve(TargetPeriod, TargetUnitRef, traceData)) { result =>
          result.left.value shouldBe failureMessage
        }
      }
    }
  }
}
