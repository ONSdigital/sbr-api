package controllers.v1

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FreeSpec, Matchers }
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.JsString
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repository.sbrctrl.RestAdminDataUnitLinksEditRepository
import services._
import uk.gov.ons.sbr.models._
import uk.gov.ons.sbr.models.edit._

import scala.concurrent.Future

class EditVatParentLinkStatusMappingSpec extends FreeSpec with GuiceOneAppPerSuite with Matchers with MockFactory with ScalaFutures {

  private trait Fixture {
    val ValidVatRef = "397585634298"
    val TargetPeriod = Period.fromString("201803")
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

    val TargetEditParentLink = EditParentLink(
      Parent(
        IdAndType(UnitId(TargetFromLEU), UnitType.LegalUnit),
        IdAndType(UnitId(TargetToLEU), UnitType.LegalUnit)
      ),
      Map("username" -> "abcd")
    )

    val patch = Seq(
      TestOperation(Path("/parents/", "LEU"), JsString(TargetFromLEU)),
      ReplaceOperation(Path("/parents/", "LEU"), JsString(TargetToLEU))
    )

    val editService = mock[EditService]
    val repository = mock[RestAdminDataUnitLinksEditRepository]
    val controller = new AdminDataParentLinkEditController(editService)

    implicit lazy val materializer = app.materializer
  }

  "A request to edit a VAT parent LEU unit link by VAT reference and period" - {
    "is successful" in new Fixture {
      (editService.editVatParentUnitLink _).expects(TargetPeriod, TargetVatRef, TargetEditParentLink).returning(
        Future.successful(EditSuccess)
      )

      val action = controller.editVatParentLink(Period.asString(TargetPeriod), ValidVatRef)
      val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON).withBody(VATEditParentLinkPostBody)
      val response = call(action, request)

      status(response) shouldBe CREATED
    }

    "is not found" in new Fixture {
      (editService.editVatParentUnitLink _).expects(TargetPeriod, TargetVatRef, TargetEditParentLink).returning(
        Future.successful(EditUnitNotFound)
      )

      val action = controller.editVatParentLink(Period.asString(TargetPeriod), ValidVatRef)
      val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON).withBody(VATEditParentLinkPostBody)
      val response = call(action, request)

      status(response) shouldBe NOT_FOUND
    }

    "is in conflict with another edit" in new Fixture {
      (editService.editVatParentUnitLink _).expects(TargetPeriod, TargetVatRef, TargetEditParentLink).returning(
        Future.successful(EditConflict)
      )

      val action = controller.editVatParentLink(Period.asString(TargetPeriod), ValidVatRef)
      val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON).withBody(VATEditParentLinkPostBody)
      val response = call(action, request)

      status(response) shouldBe CONFLICT
    }

    "is rejected" in new Fixture {
      (editService.editVatParentUnitLink _).expects(TargetPeriod, TargetVatRef, TargetEditParentLink).returning(
        Future.successful(EditRejected)
      )

      val action = controller.editVatParentLink(Period.asString(TargetPeriod), ValidVatRef)
      val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON).withBody(VATEditParentLinkPostBody)
      val response = call(action, request)

      status(response) shouldBe UNPROCESSABLE_ENTITY
    }

    "fails" in new Fixture {
      (editService.editVatParentUnitLink _).expects(TargetPeriod, TargetVatRef, TargetEditParentLink).returning(
        Future.successful(EditFailure)
      )

      val action = controller.editVatParentLink(Period.asString(TargetPeriod), ValidVatRef)
      val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON).withBody(VATEditParentLinkPostBody)
      val response = call(action, request)

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}