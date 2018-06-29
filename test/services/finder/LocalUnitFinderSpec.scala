package services.finder

import java.time.Month.JUNE

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ EitherValues, FreeSpec, Matchers }
import play.api.libs.json.{ JsObject, Json }
import repository.LocalUnitRepository
import uk.gov.ons.sbr.models.UnitType.{ Enterprise, LegalUnit, LocalUnit }
import uk.gov.ons.sbr.models._
import unitref.UnitRef

import scala.concurrent.Future

class LocalUnitFinderSpec extends FreeSpec with Matchers with MockFactory with ScalaFutures with EitherValues {

  private trait Fixture {
    val TargetPeriod = Period.fromYearMonth(2018, JUNE)
    val TargetLurn = Lurn("123456789")
    val ParentErn = Ern("9876543210")
    val LocalUnitLinks = UnitLinks(
      id = UnitId(TargetLurn.value),
      unitType = LocalUnit,
      period = TargetPeriod,
      parents = Some(Map(Enterprise -> UnitId(ParentErn.value))),
      children = None
    )
    val UnitJson = Json.parse(s"""{"some":"json"}""").as[JsObject]

    val localUnitRepository = mock[LocalUnitRepository]
    val enterpriseUnitRefType = stub[UnitRef[Ern]]
    (enterpriseUnitRefType.fromUnitId _).when(UnitId(ParentErn.value)).returns(ParentErn)
    val finder = new LocalUnitFinder(localUnitRepository, enterpriseUnitRefType)
  }

  "A Local Unit finder" - {
    "retrieves a local unit via the repository when the unit links contain a parent Enterprise" - {
      "returning the JSON representation when the local unit is found" in new Fixture {
        (localUnitRepository.retrieveLocalUnit _).expects(TargetPeriod, ParentErn, TargetLurn).returning(
          Future.successful(Right(Some(UnitJson)))
        )

        whenReady(finder.find(TargetPeriod, TargetLurn, LocalUnitLinks)) { result =>
          result.right.value shouldBe Some(UnitJson)
        }
      }

      "returning nothing when the local unit is not found" in new Fixture {
        (localUnitRepository.retrieveLocalUnit _).expects(TargetPeriod, ParentErn, TargetLurn).returning(
          Future.successful(Right(None))
        )

        whenReady(finder.find(TargetPeriod, TargetLurn, LocalUnitLinks)) { result =>
          result.right.value shouldBe empty
        }
      }

      "returning the failure message when the retrieval fails" in new Fixture {
        val failureMessage = "retrieval failed"
        (localUnitRepository.retrieveLocalUnit _).expects(TargetPeriod, ParentErn, TargetLurn).returning(
          Future.successful(Left(failureMessage))
        )

        whenReady(finder.find(TargetPeriod, TargetLurn, LocalUnitLinks)) { result =>
          result.left.value shouldBe failureMessage
        }
      }
    }

    "fails when the unit links do not contain a parent Enterprise" in new Fixture {
      val unitLinksMissingParentEnterprise = LocalUnitLinks.copy(parents = Some(Map(LegalUnit -> UnitId("1234567890123456"))))

      whenReady(finder.find(TargetPeriod, TargetLurn, unitLinksMissingParentEnterprise)) { result =>
        result.left.value shouldBe s"Unit Links for Local Unit [$TargetLurn] is missing a parent Enterprise."
      }
    }
  }
}
