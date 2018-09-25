package repository.sbrctrl

import java.time.Month.MAY

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ EitherValues, FreeSpec, Matchers }
import play.api.libs.json.JsString
import repository.rest._
import uk.gov.ons.sbr.models._
import uk.gov.ons.sbr.models.UnitType.LegalUnit
import uk.gov.ons.sbr.models.edit.OperationTypes.{ Add, Replace, Test }
import uk.gov.ons.sbr.models.edit.{ Operation, Patch, Path }

import scala.concurrent.Future

class RestAdminDataUnitLinksEditRepositorySpec extends FreeSpec with Matchers with MockFactory with ScalaFutures with EitherValues {

  private trait Fixture {
    val TargetPeriod = Period.fromYearMonth(2018, MAY)
    val TargetVAT = VatRef("123456789012")
    val TargetFromUBRN = UnitId("123456789")
    val TargetToUBRN = UnitId("987654321")

    val PatchTestReplaceParentLink: Patch = Seq(
      Operation(Test, Path("/parents/", "LEU"), JsString(TargetFromUBRN.value)),
      Operation(Replace, Path("/parents/", "LEU"), JsString(TargetToUBRN.value))
    )

    val PatchCreateParentLink: Patch = Seq(
      Operation(Add, Path("/children/", TargetVAT.value), JsString("VAT"))
    )

    val from = IdAndType(TargetFromUBRN, LegalUnit)
    val to = IdAndType(TargetToUBRN, LegalUnit)

    val unitKey = UnitKey(TargetToUBRN, LegalUnit, TargetPeriod)

    val unitRepository = mock[Repository]
    val editAdminDataRepository = new RestAdminDataUnitLinksEditRepository(unitRepository)
  }

  "An AdminDataUnitLinksEdit repository" - {
    "returns an EditSuccess" - {
      "for a request to update a parent unit link" in new Fixture {
        (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/VAT/units/${TargetVAT.value}", PatchTestReplaceParentLink).returning(
          Future.successful(PatchSuccess)
        )

        whenReady(editAdminDataRepository.updateVatParentUnitLink(from, to, TargetVAT, TargetPeriod)) { result =>
          result shouldBe PatchSuccess
        }
      }

      "for a request to create a child unit link" in new Fixture {
        (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/LEU/units/${TargetToUBRN.value}", PatchCreateParentLink).returning(
          Future.successful(PatchSuccess)
        )

        whenReady(editAdminDataRepository.createLeuChildUnitLink(unitKey, TargetVAT)) { result =>
          result shouldBe PatchSuccess
        }
      }
    }

    "returns an EditRejected when an edit is rejected (due to valid but unprocessable JSON)" in new Fixture {
      (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/VAT/units/${TargetVAT.value}", PatchTestReplaceParentLink).returning(
        Future.successful(PatchRejected)
      )

      whenReady(editAdminDataRepository.updateVatParentUnitLink(from, to, TargetVAT, TargetPeriod)) { result =>
        result shouldBe PatchRejected
      }
    }

    "returns an EditUnitNotFound when the target unit cannot be found" in new Fixture {
      (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/VAT/units/${TargetVAT.value}", PatchTestReplaceParentLink).returning(
        Future.successful(PatchUnitNotFound)
      )

      whenReady(editAdminDataRepository.updateVatParentUnitLink(from, to, TargetVAT, TargetPeriod)) { result =>
        result shouldBe PatchUnitNotFound
      }
    }

    "returns an EditConflict when the target unit has been edited by another person" in new Fixture {
      (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/VAT/units/${TargetVAT.value}", PatchTestReplaceParentLink).returning(
        Future.successful(PatchConflict)
      )

      whenReady(editAdminDataRepository.updateVatParentUnitLink(from, to, TargetVAT, TargetPeriod)) { result =>
        result shouldBe PatchConflict
      }
    }

    "returns an EditFailure when a general error has occurred" in new Fixture {
      (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/VAT/units/${TargetVAT.value}", PatchTestReplaceParentLink).returning(
        Future.successful(PatchFailure)
      )

      whenReady(editAdminDataRepository.updateVatParentUnitLink(from, to, TargetVAT, TargetPeriod)) { result =>
        result shouldBe PatchFailure
      }
    }
  }
}