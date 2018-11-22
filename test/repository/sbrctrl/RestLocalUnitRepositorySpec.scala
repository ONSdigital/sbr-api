package repository.sbrctrl

import java.time.Month.MAY

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{EitherValues, FreeSpec, Matchers}
import play.api.libs.json.Json
import repository.rest.Repository
import support.sample.SampleLocalUnit
import tracing.TraceData
import uk.gov.ons.sbr.models.{Ern, Lurn, Period}

import scala.concurrent.{ExecutionContext, Future}

class RestLocalUnitRepositorySpec extends FreeSpec with Matchers with MockFactory with ScalaFutures with EitherValues {

  private trait Fixture {
    val TargetPeriod = Period.fromYearMonth(2018, MAY)
    val TargetErn = Ern("9876543210")
    val TargetLurn = Lurn("123456789")
    val LocalUnitJson = SampleLocalUnit.asJson(TargetErn, TargetLurn)
    val SpanName = "get-local-unit"

    val traceData = stub[TraceData]
    val unitRepository = mock[Repository]
    val localUnitRepository = new RestLocalUnitRepository(unitRepository)(ExecutionContext.global)
  }

  "A LocalUnit repository" - {
    "returns the requested local unit when found" in new Fixture {
      (unitRepository.getJson _).expects(s"v1/enterprises/${TargetErn.value}/periods/${Period.asString(TargetPeriod)}/localunits/${TargetLurn.value}", SpanName, traceData).returning(
        Future.successful(Right(Some(LocalUnitJson)))
      )

      whenReady(localUnitRepository.retrieveLocalUnit(TargetPeriod, TargetErn, TargetLurn, traceData)) { result =>
        result.right.value shouldBe Some(LocalUnitJson)
      }
    }

    "returns nothing when the requested local unit is not found" in new Fixture {
      (unitRepository.getJson _).expects(s"v1/enterprises/${TargetErn.value}/periods/${Period.asString(TargetPeriod)}/localunits/${TargetLurn.value}", SpanName, traceData).returning(
        Future.successful(Right(None))
      )

      whenReady(localUnitRepository.retrieveLocalUnit(TargetPeriod, TargetErn, TargetLurn, traceData)) { result =>
        result.right.value shouldBe empty
      }
    }

    "returns an error message" - {
      "when the retrieval fails" in new Fixture {
        val failureMessage = "some failure message"
        (unitRepository.getJson _).expects(s"v1/enterprises/${TargetErn.value}/periods/${Period.asString(TargetPeriod)}/localunits/${TargetLurn.value}", SpanName, traceData).returning(
          Future.successful(Left(failureMessage))
        )

        whenReady(localUnitRepository.retrieveLocalUnit(TargetPeriod, TargetErn, TargetLurn, traceData)) { result =>
          result.left.value shouldBe failureMessage
        }
      }

      "when the JSON received is not a Json object" in new Fixture {
        val jsArray = Json.parse(s"""[{"lurn":"${TargetLurn.value}"}]""")
        (unitRepository.getJson _).expects(s"v1/enterprises/${TargetErn.value}/periods/${Period.asString(TargetPeriod)}/localunits/${TargetLurn.value}", SpanName, traceData).returning(
          Future.successful(Right(Some(jsArray)))
        )

        whenReady(localUnitRepository.retrieveLocalUnit(TargetPeriod, TargetErn, TargetLurn, traceData)) { result =>
          result.left.value should startWith("Unable to parse json response")
        }
      }
    }
  }
}
