package repository.sbrctrl

import java.time.Month.MAY

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ EitherValues, FreeSpec, Matchers }
import play.api.libs.json.JsString
import repository.rest.{ PatchRejected, PatchUnitNotFound, _ }
import uk.gov.ons.sbr.models._
import uk.gov.ons.sbr.models.UnitType.{ LegalUnit, ValueAddedTax }
import uk.gov.ons.sbr.models.edit._

import scala.concurrent.Future

class RestAdminDataUnitLinksEditRepositorySpec extends FreeSpec with Matchers with MockFactory with ScalaFutures with EitherValues {

  private trait Fixture {
    val TargetPeriod = Period.fromYearMonth(2018, MAY)
    val TargetVAT = VatRef("123456789012")
    val TargetFromUBRN = UnitId("123456789")
    val TargetToUBRN = UnitId("987654321")

    val PatchTestReplaceParentLink: Patch = Seq(
      TestOperation(Path("/parents/", "LEU"), JsString(TargetFromUBRN.value)),
      ReplaceOperation(Path("/parents/", "LEU"), JsString(TargetToUBRN.value))
    )

    val PatchCreateParentLink: Patch = Seq(
      AddOperation(Path("/children/", TargetVAT.value), JsString("VAT"))
    )

    val PatchDeleteChildLink: Patch = Seq(
      TestOperation(Path("/children/", TargetVAT.value), JsString("VAT")),
      RemoveOperation(Path("/children/", TargetVAT.value))
    )

    val TargetFromIdAndType = IdAndType(TargetFromUBRN, LegalUnit)
    val TargetToIdAndType = IdAndType(TargetToUBRN, LegalUnit)

    val TargetUpdateUnitKey = UnitKey(UnitId(TargetVAT.value), ValueAddedTax, TargetPeriod)
    val TargetCreateUnitKey = UnitKey(TargetToUBRN, LegalUnit, TargetPeriod)
    val TargetDeleteUnitKey = UnitKey(TargetFromUBRN, LegalUnit, TargetPeriod)
    val TargetVATIdAndType = IdAndType(UnitId(TargetVAT.value), ValueAddedTax)

    val unitRepository = mock[Repository]
    val editAdminDataRepository = new RestAdminDataUnitLinksEditRepository(unitRepository)
  }

  "An AdminDataUnitLinksEdit repository" - {
    "returns an EditSuccess" - {
      "for a request to update a parent unit link" in new Fixture {
        (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/VAT/units/${TargetVAT.value}", PatchTestReplaceParentLink).returning(
          Future.successful(PatchSuccess)
        )

        whenReady(editAdminDataRepository.updateVatParentUnitLink(TargetUpdateUnitKey, TargetFromIdAndType, TargetToIdAndType)) { result =>
          result shouldBe PatchSuccess
        }
      }

      "for a request to create a child unit link" in new Fixture {
        (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/LEU/units/${TargetToUBRN.value}", PatchCreateParentLink).returning(
          Future.successful(PatchSuccess)
        )

        whenReady(editAdminDataRepository.createLeuChildUnitLink(TargetCreateUnitKey, TargetVATIdAndType)) { result =>
          result shouldBe PatchSuccess
        }
      }

      "for a request to delete a child unit link" in new Fixture {
        (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/LEU/units/${TargetFromUBRN.value}", PatchDeleteChildLink).returning(
          Future.successful(PatchSuccess)
        )

        whenReady(editAdminDataRepository.deleteLeuChildUnitLink(TargetDeleteUnitKey, TargetVATIdAndType)) { result =>
          result shouldBe PatchSuccess
        }
      }
    }

    "returns an EditRejected when an edit is rejected (due to valid but unprocessable JSON)" - {
      "for a request to update a parent unit link" in new Fixture {
        (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/VAT/units/${TargetVAT.value}", PatchTestReplaceParentLink).returning(
          Future.successful(PatchRejected)
        )

        whenReady(editAdminDataRepository.updateVatParentUnitLink(TargetUpdateUnitKey, TargetFromIdAndType, TargetToIdAndType)) { result =>
          result shouldBe PatchRejected
        }
      }

      "for a request to create a child unit link" in new Fixture {
        (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/LEU/units/${TargetToUBRN.value}", PatchCreateParentLink).returning(
          Future.successful(PatchRejected)
        )

        whenReady(editAdminDataRepository.createLeuChildUnitLink(TargetCreateUnitKey, TargetVATIdAndType)) { result =>
          result shouldBe PatchRejected
        }
      }

      "for a request to delete a child unit link" in new Fixture {
        (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/LEU/units/${TargetFromUBRN.value}", PatchDeleteChildLink).returning(
          Future.successful(PatchRejected)
        )

        whenReady(editAdminDataRepository.deleteLeuChildUnitLink(TargetDeleteUnitKey, TargetVATIdAndType)) { result =>
          result shouldBe PatchRejected
        }
      }
    }

    "returns an EditUnitNotFound when the target unit cannot be found" - {
      "for a request to update a parent unit link" in new Fixture {
        (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/VAT/units/${TargetVAT.value}", PatchTestReplaceParentLink).returning(
          Future.successful(PatchUnitNotFound)
        )

        whenReady(editAdminDataRepository.updateVatParentUnitLink(TargetUpdateUnitKey, TargetFromIdAndType, TargetToIdAndType)) { result =>
          result shouldBe PatchUnitNotFound
        }
      }

      "for a request to create a child unit link" in new Fixture {
        (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/LEU/units/${TargetToUBRN.value}", PatchCreateParentLink).returning(
          Future.successful(PatchUnitNotFound)
        )

        whenReady(editAdminDataRepository.createLeuChildUnitLink(TargetCreateUnitKey, TargetVATIdAndType)) { result =>
          result shouldBe PatchUnitNotFound
        }
      }

      "for a request to delete a child unit link" in new Fixture {
        (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/LEU/units/${TargetFromUBRN.value}", PatchDeleteChildLink).returning(
          Future.successful(PatchUnitNotFound)
        )

        whenReady(editAdminDataRepository.deleteLeuChildUnitLink(TargetDeleteUnitKey, TargetVATIdAndType)) { result =>
          result shouldBe PatchUnitNotFound
        }
      }
    }

    "returns an EditConflict when the target unit has been edited by another person" - {
      "for a request to update a parent unit link" in new Fixture {
        (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/VAT/units/${TargetVAT.value}", PatchTestReplaceParentLink).returning(
          Future.successful(PatchConflict)
        )

        whenReady(editAdminDataRepository.updateVatParentUnitLink(TargetUpdateUnitKey, TargetFromIdAndType, TargetToIdAndType)) { result =>
          result shouldBe PatchConflict
        }
      }

      "for a request to create a child unit link" in new Fixture {
        (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/LEU/units/${TargetToUBRN.value}", PatchCreateParentLink).returning(
          Future.successful(PatchConflict)
        )

        whenReady(editAdminDataRepository.createLeuChildUnitLink(TargetCreateUnitKey, TargetVATIdAndType)) { result =>
          result shouldBe PatchConflict
        }
      }

      "for a request to delete a child unit link" in new Fixture {
        (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/LEU/units/${TargetFromUBRN.value}", PatchDeleteChildLink).returning(
          Future.successful(PatchConflict)
        )

        whenReady(editAdminDataRepository.deleteLeuChildUnitLink(TargetDeleteUnitKey, TargetVATIdAndType)) { result =>
          result shouldBe PatchConflict
        }
      }
    }

    "returns an EditFailure when a general error has occurred" - {
      "for a request to update a parent unit link" in new Fixture {
        (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/VAT/units/${TargetVAT.value}", PatchTestReplaceParentLink).returning(
          Future.successful(PatchFailure)
        )

        whenReady(editAdminDataRepository.updateVatParentUnitLink(TargetUpdateUnitKey, TargetFromIdAndType, TargetToIdAndType)) { result =>
          result shouldBe PatchFailure
        }
      }

      "for a request to create a child unit link" in new Fixture {
        (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/LEU/units/${TargetToUBRN.value}", PatchCreateParentLink).returning(
          Future.successful(PatchFailure)
        )

        whenReady(editAdminDataRepository.createLeuChildUnitLink(TargetCreateUnitKey, TargetVATIdAndType)) { result =>
          result shouldBe PatchFailure
        }
      }

      "for a request to delete a child unit link" in new Fixture {
        (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/LEU/units/${TargetFromUBRN.value}", PatchDeleteChildLink).returning(
          Future.successful(PatchFailure)
        )

        whenReady(editAdminDataRepository.deleteLeuChildUnitLink(TargetDeleteUnitKey, TargetVATIdAndType)) { result =>
          result shouldBe PatchFailure
        }
      }
    }
  }
}