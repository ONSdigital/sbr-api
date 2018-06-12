package repository.sbrctrl

import java.time.Month.APRIL

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ EitherValues, FreeSpec, Matchers }
import play.api.libs.json.Json
import repository.rest.UnitRepository
import support.sample.SampleEnterprise
import uk.gov.ons.sbr.models.{ Ern, Period }

import scala.concurrent.Future

class RestEnterpriseRepositorySpec extends FreeSpec with Matchers with MockFactory with ScalaFutures with EitherValues {

  private trait Fixture {
    val TargetPeriod = Period.fromYearMonth(2018, APRIL)
    val TargetErn = Ern("9876543210")
    val EnterpriseJson = SampleEnterprise.asJson(TargetErn)

    val unitRepository = mock[UnitRepository]
    val enterpriseRepository = new RestEnterpriseRepository(unitRepository)
  }

  "An Enterprise repository" - {
    "returns the requested enterprise when found" in new Fixture {
      (unitRepository.getJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/enterprises/${TargetErn.value}").returning(
        Future.successful(Right(Some(EnterpriseJson)))
      )

      whenReady(enterpriseRepository.retrieveEnterprise(TargetPeriod, TargetErn)) { result =>
        result.right.value shouldBe Some(EnterpriseJson)
      }
    }

    "returns nothing when the requested enterprise is not found" in new Fixture {
      (unitRepository.getJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/enterprises/${TargetErn.value}").returning(
        Future.successful(Right(None))
      )

      whenReady(enterpriseRepository.retrieveEnterprise(TargetPeriod, TargetErn)) { result =>
        result.right.value shouldBe empty
      }
    }

    "returns an error message" - {
      "when the retrieval fails" in new Fixture {
        val failureMessage = "some failure message"
        (unitRepository.getJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/enterprises/${TargetErn.value}").returning(
          Future.successful(Left(failureMessage))
        )

        whenReady(enterpriseRepository.retrieveEnterprise(TargetPeriod, TargetErn)) { result =>
          result.left.value shouldBe failureMessage
        }
      }

      "when the JSON received is not a Json object" in new Fixture {
        val jsArray = Json.parse(s"""[{"ern":"${TargetErn.value}"}]""")
        (unitRepository.getJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/enterprises/${TargetErn.value}").returning(
          Future.successful(Right(Some(jsArray)))
        )

        whenReady(enterpriseRepository.retrieveEnterprise(TargetPeriod, TargetErn)) { result =>
          result.left.value should startWith("Json returned for Enterprise does not define an object")
        }
      }
    }
  }
}
