package controllers.v1

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, Matchers}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsers.JsonUnitLinkEditBodyParser
import play.api.libs.json.JsString
import play.api.test.{FakeRequest, StubControllerComponentsFactory, StubPlayBodyParsersFactory}
import play.api.test.Helpers._
import repository.sbrctrl.RestAdminDataUnitLinksEditRepository
import services._
import uk.gov.ons.sbr.models._
import uk.gov.ons.sbr.models.edit._

import scala.concurrent.{ExecutionContext, Future}

class EditAdminDataParentLinkStatusMappingSpec extends FreeSpec with GuiceOneAppPerSuite with Matchers with MockFactory with ScalaFutures {

  private trait Fixture extends StubControllerComponentsFactory with StubPlayBodyParsersFactory {
    val ValidVatRef = "397585634298"
    val ValidPayeRef = "192039485761"
    val TargetPeriod = Period.fromString("201803")
    val TargetVatRef = VatRef(ValidVatRef)
    val TargetPayeRef = PayeRef(ValidPayeRef)
    val TargetPath = "/parents/LEU"
    val TargetFromLEU = "123456789"
    val TargetToLEU = "234567890"

    val EditParentLinkPostBody =
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

    implicit lazy val materializer = app.materializer
    val editParentLinkBodyParser = new JsonUnitLinkEditBodyParser(stubPlayBodyParsers.json)(ExecutionContext.global)
    val controller = new AdminDataParentLinkEditController(editParentLinkBodyParser, editService, stubControllerComponents())
  }

  "A request to edit a parent LEU unit link by reference number and period" - {
    "is successful" - {
      "for a VAT reference" in new Fixture {
        (editService.editVatAdminDataParentUnitLink _).expects(TargetPeriod, TargetVatRef, TargetEditParentLink).returning(
          Future.successful(EditSuccess)
        )

        val action = controller.editVatParentLink(Period.asString(TargetPeriod), ValidVatRef)
        val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON).withBody(EditParentLinkPostBody)
        val response = call(action, request)

        status(response) shouldBe CREATED
      }

      "for a PAYE reference" in new Fixture {
        (editService.editPayeAdminDataParentUnitLink _).expects(TargetPeriod, TargetPayeRef, TargetEditParentLink).returning(
          Future.successful(EditSuccess)
        )

        val action = controller.editPayeParentLink(Period.asString(TargetPeriod), ValidPayeRef)
        val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON).withBody(EditParentLinkPostBody)
        val response = call(action, request)

        status(response) shouldBe CREATED
      }
    }

    "is not found" - {
      "for a VAT reference" in new Fixture {
        (editService.editVatAdminDataParentUnitLink _).expects(TargetPeriod, TargetVatRef, TargetEditParentLink).returning(
          Future.successful(EditUnitNotFound)
        )

        val action = controller.editVatParentLink(Period.asString(TargetPeriod), ValidVatRef)
        val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON).withBody(EditParentLinkPostBody)
        val response = call(action, request)

        status(response) shouldBe NOT_FOUND
      }

      "for a PAYE reference" in new Fixture {
        (editService.editPayeAdminDataParentUnitLink _).expects(TargetPeriod, TargetPayeRef, TargetEditParentLink).returning(
          Future.successful(EditUnitNotFound)
        )

        val action = controller.editPayeParentLink(Period.asString(TargetPeriod), ValidPayeRef)
        val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON).withBody(EditParentLinkPostBody)
        val response = call(action, request)

        status(response) shouldBe NOT_FOUND
      }
    }

    "is in conflict with another edit" - {
      "for a VAT reference" in new Fixture {
        (editService.editVatAdminDataParentUnitLink _).expects(TargetPeriod, TargetVatRef, TargetEditParentLink).returning(
          Future.successful(EditConflict)
        )

        val action = controller.editVatParentLink(Period.asString(TargetPeriod), ValidVatRef)
        val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON).withBody(EditParentLinkPostBody)
        val response = call(action, request)

        status(response) shouldBe CONFLICT
      }

      "for a PAYE reference" in new Fixture {
        (editService.editPayeAdminDataParentUnitLink _).expects(TargetPeriod, TargetPayeRef, TargetEditParentLink).returning(
          Future.successful(EditConflict)
        )

        val action = controller.editPayeParentLink(Period.asString(TargetPeriod), ValidPayeRef)
        val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON).withBody(EditParentLinkPostBody)
        val response = call(action, request)

        status(response) shouldBe CONFLICT
      }
    }

    "is rejected" - {
      "for a VAT reference" in new Fixture {
        (editService.editVatAdminDataParentUnitLink _).expects(TargetPeriod, TargetVatRef, TargetEditParentLink).returning(
          Future.successful(EditRejected)
        )

        val action = controller.editVatParentLink(Period.asString(TargetPeriod), ValidVatRef)
        val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON).withBody(EditParentLinkPostBody)
        val response = call(action, request)

        status(response) shouldBe UNPROCESSABLE_ENTITY
      }

      "for a PAYE reference" in new Fixture {
        (editService.editPayeAdminDataParentUnitLink _).expects(TargetPeriod, TargetPayeRef, TargetEditParentLink).returning(
          Future.successful(EditRejected)
        )

        val action = controller.editPayeParentLink(Period.asString(TargetPeriod), ValidPayeRef)
        val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON).withBody(EditParentLinkPostBody)
        val response = call(action, request)

        status(response) shouldBe UNPROCESSABLE_ENTITY
      }
    }

    "fails" - {
      "for a VAT reference" in new Fixture {
        (editService.editVatAdminDataParentUnitLink _).expects(TargetPeriod, TargetVatRef, TargetEditParentLink).returning(
          Future.successful(EditFailure)
        )

        val action = controller.editVatParentLink(Period.asString(TargetPeriod), ValidVatRef)
        val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON).withBody(EditParentLinkPostBody)
        val response = call(action, request)

        status(response) shouldBe INTERNAL_SERVER_ERROR
      }

      "for a PAYE reference" in new Fixture {
        (editService.editPayeAdminDataParentUnitLink _).expects(TargetPeriod, TargetPayeRef, TargetEditParentLink).returning(
          Future.successful(EditFailure)
        )

        val action = controller.editPayeParentLink(Period.asString(TargetPeriod), ValidPayeRef)
        val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON).withBody(EditParentLinkPostBody)
        val response = call(action, request)

        status(response) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}