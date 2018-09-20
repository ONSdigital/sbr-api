package controllers.v1

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FreeSpec, Matchers }
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{ JsString, Json }
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repository._
import repository.sbrctrl.RestAdminDataUnitLinksEditRepository
import services.PatchCreationService
import uk.gov.ons.sbr.models._
import uk.gov.ons.sbr.models.edit.{ Operation, OperationTypes }

import scala.concurrent.Future

class EditVatParentLinkStatusMappingSpec extends FreeSpec with GuiceOneAppPerSuite with Matchers with MockFactory with ScalaFutures {

  private trait Fixture {
    val ValidVatRef = "397585634298"
    val ValidPeriod = "201803"
    val TargetPeriod = Period.fromString(ValidPeriod)
    val TargetVatRef = VatRef(ValidVatRef)
    val TargetPath = "/parents/LEU"
    val TargetFromLEU = "123456789"
    val TargetToLEU = "234567890"

    val VATEditParentLinkPostBody =
      s"""{
          |  "parent": {
          |    "from": {
          |      "id": "$TargetFromLEU",
          |      "type": "LEU"
          |    },
          |    "to": {
          |      "id": "$TargetToLEU",
          |      "type": "LEU"
          |    }
          |  },
          |  "audit": { "username": "abcd" }
          |}""".stripMargin

    val editParentLink = EditParentLink(
      Parent(
        IdAndType(UnitId(TargetFromLEU), UnitType.LegalUnit),
        IdAndType(UnitId(TargetToLEU), UnitType.LegalUnit)
      ),
      Map("username" -> "abcd")
    )

    val patch = Seq(
      Operation(OperationTypes.Test, TargetPath, JsString(TargetFromLEU)),
      Operation(OperationTypes.Replace, TargetPath, JsString(TargetToLEU))
    )

    val patchCreation = mock[PatchCreationService]
    val repository = mock[RestAdminDataUnitLinksEditRepository]
    val controller = new AdminDataParentLinkEditController(patchCreation, repository)

    implicit lazy val materializer = app.materializer
  }

  "A request to edit a VAT parent LEU unit link by VAT reference and period" - {
    "is successful" in new Fixture {
      (patchCreation.createPatch _).expects(editParentLink).returning(Right(patch))
      (repository.patchVatParentUnitLink _).expects(patch, TargetPeriod, TargetVatRef).returning(
        Future.successful(EditSuccess)
      )

      val action = controller.editVatParentLink(ValidPeriod, ValidVatRef)
      val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON).withJsonBody(Json.toJson(editParentLink))
      val response = call(action, request)

      status(response) shouldBe CREATED
    }

    "is not found" in new Fixture {
      (patchCreation.createPatch _).expects(editParentLink).returning(Right(patch))
      (repository.patchVatParentUnitLink _).expects(patch, TargetPeriod, TargetVatRef).returning(
        Future.successful(EditUnitNotFound)
      )

      val action = controller.editVatParentLink(ValidPeriod, ValidVatRef)
      val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON).withJsonBody(Json.toJson(editParentLink))
      val response = call(action, request)

      status(response) shouldBe NOT_FOUND
    }

    "is in conflict with another edit" in new Fixture {
      (patchCreation.createPatch _).expects(editParentLink).returning(Right(patch))
      (repository.patchVatParentUnitLink _).expects(patch, TargetPeriod, TargetVatRef).returning(
        Future.successful(EditConflict)
      )

      val action = controller.editVatParentLink(ValidPeriod, ValidVatRef)
      val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON).withJsonBody(Json.toJson(editParentLink))
      val response = call(action, request)

      status(response) shouldBe CONFLICT
    }

    "is rejected" in new Fixture {
      (patchCreation.createPatch _).expects(editParentLink).returning(Right(patch))
      (repository.patchVatParentUnitLink _).expects(patch, TargetPeriod, TargetVatRef).returning(
        Future.successful(EditRejected)
      )

      val action = controller.editVatParentLink(ValidPeriod, ValidVatRef)
      val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON).withJsonBody(Json.toJson(editParentLink))
      val response = call(action, request)

      status(response) shouldBe UNPROCESSABLE_ENTITY
    }

    "fails" in new Fixture {
      (patchCreation.createPatch _).expects(editParentLink).returning(Right(patch))
      (repository.patchVatParentUnitLink _).expects(patch, TargetPeriod, TargetVatRef).returning(
        Future.successful(EditFailure)
      )

      val action = controller.editVatParentLink(ValidPeriod, ValidVatRef)
      val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON).withJsonBody(Json.toJson(editParentLink))
      val response = call(action, request)

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}