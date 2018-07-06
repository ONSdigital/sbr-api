package repository.sbrctrl

import java.time.Month.MAY

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ EitherValues, FreeSpec, Matchers }
import play.api.libs.json.Json
import repository.rest.Repository
import support.sample.SampleReportingUnit
import tracing.TraceData
import uk.gov.ons.sbr.models.{ Ern, Period, Rurn }

import scala.concurrent.Future

class RestReportingUnitRepositorySpec extends FreeSpec with Matchers with MockFactory with ScalaFutures with EitherValues {

  private trait Fixture {
    val TargetPeriod = Period.fromYearMonth(2018, MAY)
    val TargetErn = Ern("9876543210")
    val TargetRurn = Rurn("12345678901")
    val ReportingUnitJson = SampleReportingUnit.asJson(TargetErn, TargetRurn)
    val SpanName = "get-reporting-unit"

    val traceData = stub[TraceData]
    val unitRepository = mock[Repository]
    val reportingUnitRepository = new RestReportingUnitRepository(unitRepository)
  }

  "A ReportingUnit repository" - {
    "returns the requested reporting unit when found" in new Fixture {
      (unitRepository.getJson _).expects(s"v1/enterprises/${TargetErn.value}/periods/${Period.asString(TargetPeriod)}/reportingunits/${TargetRurn.value}", SpanName, traceData).returning(
        Future.successful(Right(Some(ReportingUnitJson)))
      )

      whenReady(reportingUnitRepository.retrieveReportingUnit(TargetPeriod, TargetErn, TargetRurn, traceData)) { result =>
        result.right.value shouldBe Some(ReportingUnitJson)
      }
    }

    "returns nothing when the requested reporting unit is not found" in new Fixture {
      (unitRepository.getJson _).expects(s"v1/enterprises/${TargetErn.value}/periods/${Period.asString(TargetPeriod)}/reportingunits/${TargetRurn.value}", SpanName, traceData).returning(
        Future.successful(Right(None))
      )

      whenReady(reportingUnitRepository.retrieveReportingUnit(TargetPeriod, TargetErn, TargetRurn, traceData)) { result =>
        result.right.value shouldBe empty
      }
    }

    "returns an error message" - {
      "when the retrieval fails" in new Fixture {
        val failureMessage = "some failure message"
        (unitRepository.getJson _).expects(s"v1/enterprises/${TargetErn.value}/periods/${Period.asString(TargetPeriod)}/reportingunits/${TargetRurn.value}", SpanName, traceData).returning(
          Future.successful(Left(failureMessage))
        )

        whenReady(reportingUnitRepository.retrieveReportingUnit(TargetPeriod, TargetErn, TargetRurn, traceData)) { result =>
          result.left.value shouldBe failureMessage
        }
      }

      "when the JSON received is not a Json object" in new Fixture {
        val jsArray = Json.parse(s"""[{"rurn":"${TargetRurn.value}"}]""")
        (unitRepository.getJson _).expects(s"v1/enterprises/${TargetErn.value}/periods/${Period.asString(TargetPeriod)}/reportingunits/${TargetRurn.value}", SpanName, traceData).returning(
          Future.successful(Right(Some(jsArray)))
        )

        whenReady(reportingUnitRepository.retrieveReportingUnit(TargetPeriod, TargetErn, TargetRurn, traceData)) { result =>
          result.left.value should startWith("Unable to parse json response")
        }
      }
    }
  }
}
