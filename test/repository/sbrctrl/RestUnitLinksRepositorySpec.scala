package repository.sbrctrl

import java.time.Month.APRIL

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ EitherValues, FreeSpec, Matchers }
import play.api.libs.json.{ JsError, JsSuccess, Json, Reads }
import repository.rest.Repository
import uk.gov.ons.sbr.models.{ Period, UnitId, UnitLinks, UnitType }

import scala.concurrent.Future

class RestUnitLinksRepositorySpec extends FreeSpec with Matchers with MockFactory with ScalaFutures with EitherValues {

  private trait Fixture {
    val TargetEnterpriseId = UnitId("1234567890")
    val TargetPeriod = Period.fromYearMonth(2018, APRIL)
    val UnitLinksJsonResponse = Json.parse(s"""{"dummy":"value"}""")
    val TargetEnterpriseUnitLinks = UnitLinks(
      id = UnitId(TargetEnterpriseId.value),
      unitType = UnitType.Enterprise,
      period = TargetPeriod,
      parents = None,
      children = Some(Map(
        UnitId("987654321") -> UnitType.LocalUnit
      ))
    )

    val unitRepository = mock[Repository]
    val readsUnitLinks = mock[Reads[UnitLinks]]
    val unitLinksRepository = new RestUnitLinksRepository(unitRepository, readsUnitLinks)
  }

  "A Unit Links repository" - {
    "when requested to retrieve the unit links for an enterprise at a period in time" - {
      "returns enterprise unit links when found" in new Fixture {
        (unitRepository.getJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/ENT/units/${TargetEnterpriseId.value}").returning(
          Future.successful(Right(Some(UnitLinksJsonResponse)))
        )
        (readsUnitLinks.reads _).expects(UnitLinksJsonResponse).returning(
          JsSuccess(TargetEnterpriseUnitLinks)
        )

        whenReady(unitLinksRepository.retrieveUnitLinks(TargetEnterpriseId, UnitType.Enterprise, TargetPeriod)) { result =>
          result.right.value shouldBe Some(TargetEnterpriseUnitLinks)
        }
      }

      "returns nothing when not found" in new Fixture {
        (unitRepository.getJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/ENT/units/${TargetEnterpriseId.value}").returning(
          Future.successful(Right(None))
        )

        whenReady(unitLinksRepository.retrieveUnitLinks(TargetEnterpriseId, UnitType.Enterprise, TargetPeriod)) { result =>
          result.right.value shouldBe empty
        }
      }

      "returns an error message" - {
        "when the retrieval fails" in new Fixture {
          val failureMessage = "some failure message"
          (unitRepository.getJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/ENT/units/${TargetEnterpriseId.value}").returning(
            Future.successful(Left(failureMessage))
          )

          whenReady(unitLinksRepository.retrieveUnitLinks(TargetEnterpriseId, UnitType.Enterprise, TargetPeriod)) { result =>
            result.left.value shouldBe failureMessage
          }
        }

        "when unable to parse the json as a UnitLinks model" in new Fixture {
          (unitRepository.getJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/ENT/units/${TargetEnterpriseId.value}").returning(
            Future.successful(Right(Some(UnitLinksJsonResponse)))
          )
          (readsUnitLinks.reads _).expects(UnitLinksJsonResponse).returning(
            JsError("unexpected json format")
          )

          whenReady(unitLinksRepository.retrieveUnitLinks(TargetEnterpriseId, UnitType.Enterprise, TargetPeriod)) { result =>
            result.left.value should startWith("Unable to parse json response")
          }
        }
      }
    }
  }
}
