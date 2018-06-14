package services.sbrctrl

import java.time.Month.FEBRUARY

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ EitherValues, FreeSpec, Matchers }
import repository.{ EnterpriseRepository, UnitLinksRepository }
import support.sample.SampleEnterprise
import uk.gov.ons.sbr.models._
import unitref.EnterpriseUnitRef

import scala.concurrent.Future

class SbrCtrlEnterpriseServiceSpec extends FreeSpec with Matchers with MockFactory with ScalaFutures with EitherValues {

  private trait Fixture {
    val TargetErn = Ern("1234567890")
    val TargetPeriod = Period.fromYearMonth(2018, FEBRUARY)
    val EnterpriseJson = SampleEnterprise.asJson(TargetErn)
    val EnterpriseChildLinks = Some(Map(UnitId("123456789") -> UnitType.LocalUnit))

    val unitLinksRepository = mock[UnitLinksRepository]
    val enterpriseRepository = mock[EnterpriseRepository]
    val service = new SbrCtrlEnterpriseService(unitLinksRepository, enterpriseRepository)

    def enterpriseUnitLinks(withErn: Ern, withPeriod: Period, withChildren: Option[Map[UnitId, UnitType]]): UnitLinks =
      UnitLinks(
        EnterpriseUnitRef.toUnitId(withErn),
        UnitType.Enterprise,
        withPeriod,
        parents = None,
        withChildren
      )
  }

  "An Enterprise Service" - {
    "assembles an enterprise with its associated links" - {
      "when both the unit link and enterprise entries are found for the target Enterprise reference (ERN) and period" in new Fixture {
        (unitLinksRepository.retrieveUnitLinks _).expects(EnterpriseUnitRef.toUnitId(TargetErn), UnitType.Enterprise, TargetPeriod).returning(Future.successful(
          Right(Some(enterpriseUnitLinks(withErn = TargetErn, withPeriod = TargetPeriod, withChildren = EnterpriseChildLinks)))
        ))
        (enterpriseRepository.retrieveEnterprise _).expects(TargetPeriod, TargetErn).returning(
          Future.successful(Right(Some(EnterpriseJson)))
        )

        whenReady(service.retrieve(TargetPeriod, TargetErn)) { result =>
          result.right.value shouldBe Some(LinkedUnit(
            UnitId(TargetErn.value),
            UnitType.Enterprise,
            TargetPeriod,
            parents = None,
            EnterpriseChildLinks,
            EnterpriseJson
          ))
        }
      }
    }

    "returns nothing" - {
      "when a unit links entry for the enterprise cannot be found" in new Fixture {
        (unitLinksRepository.retrieveUnitLinks _).expects(EnterpriseUnitRef.toUnitId(TargetErn), UnitType.Enterprise, TargetPeriod).returning(
          Future.successful(Right(None))
        )

        whenReady(service.retrieve(TargetPeriod, TargetErn)) { result =>
          result.right.value shouldBe empty
        }
      }

      "when an enterprise entry cannot be found" in new Fixture {
        (unitLinksRepository.retrieveUnitLinks _).expects(EnterpriseUnitRef.toUnitId(TargetErn), UnitType.Enterprise, TargetPeriod).returning(Future.successful(
          Right(Some(enterpriseUnitLinks(withErn = TargetErn, withPeriod = TargetPeriod, withChildren = EnterpriseChildLinks)))
        ))
        (enterpriseRepository.retrieveEnterprise _).expects(TargetPeriod, TargetErn).returning(
          Future.successful(Right(None))
        )

        whenReady(service.retrieve(TargetPeriod, TargetErn)) { result =>
          result.right.value shouldBe empty
        }
      }
    }

    "returns an error message" - {
      "when a unit links retrieval fails" in new Fixture {
        val failureMessage = "unitLinks retrieval failure"
        (unitLinksRepository.retrieveUnitLinks _).expects(EnterpriseUnitRef.toUnitId(TargetErn), UnitType.Enterprise, TargetPeriod).returning(
          Future.successful(Left(failureMessage))
        )

        whenReady(service.retrieve(TargetPeriod, TargetErn)) { result =>
          result.left.value shouldBe failureMessage
        }
      }

      "when an enterprise retrieval fails" in new Fixture {
        val failureMessage = "enterprise retrieval failure"
        (unitLinksRepository.retrieveUnitLinks _).expects(EnterpriseUnitRef.toUnitId(TargetErn), UnitType.Enterprise, TargetPeriod).returning(Future.successful(
          Right(Some(enterpriseUnitLinks(withErn = TargetErn, withPeriod = TargetPeriod, withChildren = EnterpriseChildLinks)))
        ))
        (enterpriseRepository.retrieveEnterprise _).expects(TargetPeriod, TargetErn).returning(
          Future.successful(Left(failureMessage))
        )

        whenReady(service.retrieve(TargetPeriod, TargetErn)) { result =>
          result.left.value shouldBe failureMessage
        }
      }
    }
  }
}
