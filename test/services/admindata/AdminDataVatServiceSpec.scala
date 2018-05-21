package services.admindata

import java.time.Month.FEBRUARY

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ EitherValues, FreeSpec, Matchers }
import play.api.libs.json.JsObject
import repository.{ AdminDataRepository, UnitLinksRepository }
import support.sample.SampleVat
import uk.gov.ons.sbr.models._

import scala.concurrent.Future

class AdminDataVatServiceSpec extends FreeSpec with Matchers with MockFactory with ScalaFutures with EitherValues {

  private trait Fixture {
    val TargetVatRef = VatRef("346942023239")
    val TargetPeriod = Period.fromYearMonth(2018, FEBRUARY)
    val VatParentLinks: Option[Map[UnitType, UnitId]] = Some(Map(UnitType.Enterprise -> UnitId("1122334455")))
    val VatAdminDataVars = SampleVat.asJson(TargetVatRef)

    val unitLinksRepository = mock[UnitLinksRepository]
    val adminDataRepository = mock[AdminDataRepository]
    val service = new AdminDataVatService(unitLinksRepository, adminDataRepository)

    def vatUnitLinks(withVatRef: VatRef, withPeriod: Period, withParents: Option[Map[UnitType, UnitId]]): UnitLinks =
      UnitLinks(
        VatRef.asUnitId(withVatRef),
        UnitType.ValueAddedTax,
        withPeriod,
        parents = withParents,
        children = None
      )

    def vatAdminData(withVatRef: VatRef, withPeriod: Period, withVariables: JsObject): AdminData =
      AdminData(
        VatRef.asUnitId(withVatRef),
        withPeriod,
        withVariables
      )
  }

  "A VAT Service" - {
    "assembles a VAT unit with its associated links" - {
      "when both the unit link and VAT admin data are found for the target VAT reference and period" in new Fixture {
        (unitLinksRepository.retrieveUnitLinks _).expects(VatRef.asUnitId(TargetVatRef), UnitType.ValueAddedTax, TargetPeriod).returning(
          Future.successful(Right(Some(vatUnitLinks(withVatRef = TargetVatRef, withPeriod = TargetPeriod, withParents = VatParentLinks))))
        )
        (adminDataRepository.retrieveAdminData _).expects(VatRef.asUnitId(TargetVatRef), TargetPeriod).returning(
          Future.successful(Right(Some(vatAdminData(withVatRef = TargetVatRef, withPeriod = TargetPeriod, withVariables = VatAdminDataVars))))
        )

        whenReady(service.retrieve(TargetPeriod, TargetVatRef)) { result =>
          result.right.value shouldBe Some(LinkedUnit(
            UnitId(TargetVatRef.value),
            UnitType.ValueAddedTax,
            TargetPeriod,
            parents = VatParentLinks,
            children = None,
            vars = VatAdminDataVars
          ))
        }
      }
    }

    "returns nothing" - {
      "when a unit links entry for the VAT unit cannot be found" in new Fixture {
        (unitLinksRepository.retrieveUnitLinks _).expects(VatRef.asUnitId(TargetVatRef), UnitType.ValueAddedTax, TargetPeriod).returning(
          Future.successful(Right(None))
        )

        whenReady(service.retrieve(TargetPeriod, TargetVatRef)) { result =>
          result.right.value shouldBe empty
        }
      }

      "when the VAT admin data cannot be found" in new Fixture {
        (unitLinksRepository.retrieveUnitLinks _).expects(VatRef.asUnitId(TargetVatRef), UnitType.ValueAddedTax, TargetPeriod).returning(
          Future.successful(Right(Some(vatUnitLinks(withVatRef = TargetVatRef, withPeriod = TargetPeriod, withParents = VatParentLinks))))
        )
        (adminDataRepository.retrieveAdminData _).expects(VatRef.asUnitId(TargetVatRef), TargetPeriod).returning(
          Future.successful(Right(None))
        )

        whenReady(service.retrieve(TargetPeriod, TargetVatRef)) { result =>
          result.right.value shouldBe empty
        }
      }
    }

    "returns an error message" - {
      "when a unit links retrieval fails" in new Fixture {
        val failureMessage = "unitLinks retrieval failure"
        (unitLinksRepository.retrieveUnitLinks _).expects(VatRef.asUnitId(TargetVatRef), UnitType.ValueAddedTax, TargetPeriod).returning(
          Future.successful(Left(failureMessage))
        )

        whenReady(service.retrieve(TargetPeriod, TargetVatRef)) { result =>
          result.left.value shouldBe failureMessage
        }
      }

      "when a VAT admin data retrieval fails" in new Fixture {
        val failureMessage = "VAT admin data retrieval failed"
        (unitLinksRepository.retrieveUnitLinks _).expects(VatRef.asUnitId(TargetVatRef), UnitType.ValueAddedTax, TargetPeriod).returning(
          Future.successful(Right(Some(vatUnitLinks(withVatRef = TargetVatRef, withPeriod = TargetPeriod, withParents = VatParentLinks))))
        )
        (adminDataRepository.retrieveAdminData _).expects(VatRef.asUnitId(TargetVatRef), TargetPeriod).returning(
          Future.successful(Left(failureMessage))
        )

        whenReady(service.retrieve(TargetPeriod, TargetVatRef)) { result =>
          result.left.value shouldBe failureMessage
        }
      }
    }
  }
}
