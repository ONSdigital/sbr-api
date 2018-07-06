package services.finder

import java.time.Month.JUNE

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ EitherValues, FreeSpec, Matchers }
import play.api.libs.json.{ JsObject, Json }
import repository.EnterpriseRepository
import tracing.TraceData
import uk.gov.ons.sbr.models.UnitType.{ Enterprise, LegalUnit, LocalUnit }
import uk.gov.ons.sbr.models._

import scala.concurrent.Future

class EnterpriseFinderSpec extends FreeSpec with Matchers with MockFactory with ScalaFutures with EitherValues {

  private trait Fixture {
    val TargetPeriod = Period.fromYearMonth(2018, JUNE)
    val TargetErn = Ern("1234567890")
    val EnterpriseUnitLinks = UnitLinks(
      id = UnitId(TargetErn.value),
      unitType = Enterprise,
      period = TargetPeriod,
      parents = None,
      children = Some(Map(UnitId("1234567890123456") -> LegalUnit, UnitId("987654321") -> LocalUnit))
    )
    val UnitJson = Json.parse(s"""{"some":"json"}""").as[JsObject]
    val traceData = stub[TraceData]

    val enterpriseRepository = mock[EnterpriseRepository]
    val finder = new EnterpriseFinder(enterpriseRepository)
  }

  "An Enterprise finder" - {
    "retrieves an enterprise via the repository" - {
      "returning the JSON representation when the enterprise is found" in new Fixture {
        (enterpriseRepository.retrieveEnterprise _).expects(TargetPeriod, TargetErn, traceData).returning(
          Future.successful(Right(Some(UnitJson)))
        )

        whenReady(finder.find(TargetPeriod, TargetErn, EnterpriseUnitLinks, traceData)) { result =>
          result.right.value shouldBe Some(UnitJson)
        }
      }

      "returning nothing when the enterprise is not found" in new Fixture {
        (enterpriseRepository.retrieveEnterprise _).expects(TargetPeriod, TargetErn, traceData).returning(
          Future.successful(Right(None))
        )

        whenReady(finder.find(TargetPeriod, TargetErn, EnterpriseUnitLinks, traceData)) { result =>
          result.right.value shouldBe empty
        }
      }

      "returning the failure message when the retrieval fails" in new Fixture {
        val failureMessage = "retrieval failed"
        (enterpriseRepository.retrieveEnterprise _).expects(TargetPeriod, TargetErn, traceData).returning(
          Future.successful(Left(failureMessage))
        )

        whenReady(finder.find(TargetPeriod, TargetErn, EnterpriseUnitLinks, traceData)) { result =>
          result.left.value shouldBe failureMessage
        }
      }
    }
  }
}
