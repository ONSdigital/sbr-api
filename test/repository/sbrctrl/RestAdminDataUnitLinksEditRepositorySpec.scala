package repository.sbrctrl

import java.time.Month.MAY

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ EitherValues, FreeSpec, Matchers }
import play.api.libs.json.{ JsString, Json }
import repository._
import repository.rest._
import uk.gov.ons.sbr.models.edit.{ Operation, OperationTypes, Patch }
import uk.gov.ons.sbr.models.{ Period, VatRef }

import scala.concurrent.Future

class RestAdminDataUnitLinksEditRepositorySpec extends FreeSpec with Matchers with MockFactory with ScalaFutures with EitherValues {

  private trait Fixture {
    val TargetPeriod = Period.fromYearMonth(2018, MAY)
    val TargetVAT = "123456789012"
    val TargetFromUBRN = "123456789"
    val TargetToUBRN = "987654321"

    val testReplaceVatParentLink: Patch = Seq(
      Operation(OperationTypes.Test, "/parents/LEU", JsString(TargetFromUBRN)),
      Operation(OperationTypes.Replace, "/parents/LEU", JsString(TargetToUBRN))
    )

    val VATEditParentLinkPatchBody = Json.toJson(testReplaceVatParentLink)

    val unitRepository = mock[Repository]
    val editAdminDataRepository = new RestAdminDataUnitLinksEditRepository(unitRepository)
  }

  "An AdminDataUnitLinksEdit repository" - {
    "returns an EditSuccess when an edit is successful" in new Fixture {
      (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/VAT/units/$TargetVAT", testReplaceVatParentLink).returning(
        Future.successful(PatchSuccess)
      )

      whenReady(editAdminDataRepository.patchVatParentUnitLink(testReplaceVatParentLink, TargetPeriod, VatRef(TargetVAT))) { result =>
        result shouldBe EditSuccess
      }
    }

    "returns an EditRejected when an edit is rejected (due to valid but unprocessable JSON)" in new Fixture {
      (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/VAT/units/$TargetVAT", testReplaceVatParentLink).returning(
        Future.successful(PatchRejected)
      )

      whenReady(editAdminDataRepository.patchVatParentUnitLink(testReplaceVatParentLink, TargetPeriod, VatRef(TargetVAT))) { result =>
        result shouldBe EditRejected
      }
    }

    "returns an EditUnitNotFound when the target unit cannot be found" in new Fixture {
      (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/VAT/units/$TargetVAT", testReplaceVatParentLink).returning(
        Future.successful(PatchUnitNotFound)
      )

      whenReady(editAdminDataRepository.patchVatParentUnitLink(testReplaceVatParentLink, TargetPeriod, VatRef(TargetVAT))) { result =>
        result shouldBe EditUnitNotFound
      }
    }

    "returns an EditConflict when the target unit has been edited by another person" in new Fixture {
      (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/VAT/units/$TargetVAT", testReplaceVatParentLink).returning(
        Future.successful(PatchConflict)
      )

      whenReady(editAdminDataRepository.patchVatParentUnitLink(testReplaceVatParentLink, TargetPeriod, VatRef(TargetVAT))) { result =>
        result shouldBe EditConflict
      }
    }

    "returns an EditFailure when a general error has occurred" in new Fixture {
      (unitRepository.patchJson _).expects(s"v1/periods/${Period.asString(TargetPeriod)}/types/VAT/units/$TargetVAT", testReplaceVatParentLink).returning(
        Future.successful(PatchFailure)
      )

      whenReady(editAdminDataRepository.patchVatParentUnitLink(testReplaceVatParentLink, TargetPeriod, VatRef(TargetVAT))) { result =>
        result shouldBe EditFailure
      }
    }
  }
}