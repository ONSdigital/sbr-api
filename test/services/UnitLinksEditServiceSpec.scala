package services

import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FreeSpec, Matchers }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Seconds, Span }
import repository.rest._
import repository.sbrctrl.RestAdminDataUnitLinksEditRepository
import uk.gov.ons.sbr.models.UnitType.LegalUnit
import uk.gov.ons.sbr.models._

import scala.concurrent.Future

class UnitLinksEditServiceSpec extends FreeSpec with MockFactory with ScalaFutures with Matchers {

  private val TargetVat = VatRef("123456789012")
  private val TargetPeriod = Period.fromString("201803")
  private val TargetToUBRN = UnitId("1234567890123456")
  private val TargetFromUBRN = UnitId("9876543210987654")
  private val TargetFromIdAndType = IdAndType(TargetToUBRN, LegalUnit)
  private val TargetToIdAndType = IdAndType(TargetFromUBRN, LegalUnit)
  private val TargetParent = Parent(TargetFromIdAndType, TargetToIdAndType)
  private val TargetEditParent = EditParentLink(TargetParent, Map("username" -> "abcd"))
  private val TargetUnitKey = UnitKey(TargetFromUBRN, LegalUnit, TargetPeriod)

  private trait Fixture {
    val repository = mock[RestAdminDataUnitLinksEditRepository]
    val editService = new UnitLinksEditService(repository)
  }

  "a UnitLinksEditService" - {
    "returns an EditSuccess status for a request to edit a VAT units parent unit link" in new Fixture {
      (repository.updateVatParentUnitLink _).expects(TargetFromIdAndType, TargetToIdAndType, TargetVat, TargetPeriod).returning(
        Future.successful(PatchSuccess)
      )

      (repository.createLeuChildUnitLink _).expects(TargetUnitKey, TargetVat).returning(
        Future.successful(PatchSuccess)
      )

      whenReady(editService.editVatParentUnitLink(TargetPeriod, TargetVat, TargetEditParent)) { result =>
        result shouldBe EditSuccess
      }
    }

    "returns an EditUnitNotFound status when the VAT unit does not exist" in new Fixture {
      (repository.updateVatParentUnitLink _).expects(TargetFromIdAndType, TargetToIdAndType, TargetVat, TargetPeriod).returning(
        Future.successful(PatchUnitNotFound)
      )

      whenReady(editService.editVatParentUnitLink(TargetPeriod, TargetVat, TargetEditParent)) { result =>
        result shouldBe EditUnitNotFound
      }
    }

    "returns an EditConflict status when the VAT unit is being edited by somebody else" in new Fixture {
      (repository.updateVatParentUnitLink _).expects(TargetFromIdAndType, TargetToIdAndType, TargetVat, TargetPeriod).returning(
        Future.successful(PatchConflict)
      )

      whenReady(editService.editVatParentUnitLink(TargetPeriod, TargetVat, TargetEditParent)) { result =>
        result shouldBe EditConflict
      }
    }

    "returns an EditRejected status for a request to edit a VAT units parent unit link" in new Fixture {
      (repository.updateVatParentUnitLink _).expects(TargetFromIdAndType, TargetToIdAndType, TargetVat, TargetPeriod).returning(
        Future.successful(PatchRejected)
      )

      whenReady(editService.editVatParentUnitLink(TargetPeriod, TargetVat, TargetEditParent)) { result =>
        result shouldBe EditRejected
      }
    }

    "returns an EditFailure status for a request to edit a VAT units parent unit link" - {
      "when the VAT unit request fails due to problems with sbr-control-api" in new Fixture {
        (repository.updateVatParentUnitLink _).expects(TargetFromIdAndType, TargetToIdAndType, TargetVat, TargetPeriod).returning(
          Future.successful(PatchFailure)
        )

        whenReady(editService.editVatParentUnitLink(TargetPeriod, TargetVat, TargetEditParent)) { result =>
          result shouldBe EditFailure
        }
      }

      "when the VAT unit exists but the LEU unit request fails due to problems on sbr-control-api" in new Fixture {
        (repository.updateVatParentUnitLink _).expects(TargetFromIdAndType, TargetToIdAndType, TargetVat, TargetPeriod).returning(
          Future.successful(PatchSuccess)
        )

        (repository.createLeuChildUnitLink _).expects(TargetUnitKey, TargetVat).returning(
          Future.successful(PatchFailure)
        )

        whenReady(editService.editVatParentUnitLink(TargetPeriod, TargetVat, TargetEditParent), timeout(Span(1, Seconds))) { result =>
          result shouldBe EditFailure
        }
      }

      "when the VAT unit exists but the LEU unit request is rejected" in new Fixture {
        (repository.updateVatParentUnitLink _).expects(TargetFromIdAndType, TargetToIdAndType, TargetVat, TargetPeriod).returning(
          Future.successful(PatchSuccess)
        )

        (repository.createLeuChildUnitLink _).expects(TargetUnitKey, TargetVat).returning(
          Future.successful(PatchRejected)
        )

        whenReady(editService.editVatParentUnitLink(TargetPeriod, TargetVat, TargetEditParent)) { result =>
          result shouldBe EditFailure
        }
      }

      "when the VAT unit exists but the LEU unit is being edited by somebody else" in new Fixture {
        (repository.updateVatParentUnitLink _).expects(TargetFromIdAndType, TargetToIdAndType, TargetVat, TargetPeriod).returning(
          Future.successful(PatchSuccess)
        )

        (repository.createLeuChildUnitLink _).expects(TargetUnitKey, TargetVat).returning(
          Future.successful(PatchConflict)
        )

        whenReady(editService.editVatParentUnitLink(TargetPeriod, TargetVat, TargetEditParent)) { result =>
          result shouldBe EditFailure
        }
      }

      "when the VAT unit exists but the LEU unit does not exist" in new Fixture {
        (repository.updateVatParentUnitLink _).expects(TargetFromIdAndType, TargetToIdAndType, TargetVat, TargetPeriod).returning(
          Future.successful(PatchSuccess)
        )

        (repository.createLeuChildUnitLink _).expects(TargetUnitKey, TargetVat).returning(
          Future.successful(PatchUnitNotFound)
        )

        whenReady(editService.editVatParentUnitLink(TargetPeriod, TargetVat, TargetEditParent)) { result =>
          result shouldBe EditFailure
        }
      }
    }
  }
}