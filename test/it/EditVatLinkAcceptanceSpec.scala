import java.time.Month._

import com.github.tomakehurst.wiremock.client.WireMock.{ equalTo, equalToJson }
import org.scalatest.{ OptionValues, Outcome }
import play.api.http.Status.CREATED
import play.api.libs.ws.WSClient
import play.api.test.WsTestClient
import play.api.test.Helpers._
import play.api.http.{ HeaderNames, Port }
import uk.gov.ons.sbr.models._
import fixture.ServerAcceptanceSpec
import parsers.JsonUnitLinkEditBodyParser.JsonPatchMediaType
import support.wiremock.WireMockSbrControlApi

class EditVatLinkAcceptanceSpec extends ServerAcceptanceSpec with WireMockSbrControlApi with OptionValues {

  private val TargetVAT = VatRef("330000000511")
  private val ParentLEU = "1100000051"
  private val NewParentLEU = "1100000052"
  private val TargetPeriod = Period.fromYearMonth(2018, AUGUST)

  private val VATEditParentLinkPostBody =
    s"""{
       |  "parent": {
       |    "from": {
       |      "id": "$ParentLEU",
       |      "type": "LEU"
       |    },
       |    "to": {
       |      "id": "$NewParentLEU",
       |      "type": "LEU"
       |    }
       |  },
       |  "audit": { "username": "abcd" }
       |}""".stripMargin

  private val InvalidVATEditParentLinkPostBody = VATEditParentLinkPostBody + "}"

  private val UnprocessableVATEditParentLinkPostBody = VATEditParentLinkPostBody.replace("to", "from")

  private val VATEditParentLinkPatchBody =
    s"""|[
        |  { "op": "test", path: "/parents/LEU", value: "$ParentLEU" },
        |  { "op": "replace", path: "/parents/LEU", value: "$NewParentLEU" }
        |]""".stripMargin

  private val LEUCreateChildLinkPatchBody =
    s"""|[
        |  { "op": "add", path: "/children/${TargetVAT.value}", value: "VAT" }
        |]""".stripMargin

  override type FixtureParam = WSClient

  override protected def withFixture(test: OneArgTest): Outcome =
    withWireMockSbrControlApi { () =>
      WsTestClient.withClient { wsClient =>
        withFixture(test.toNoArgTest(wsClient))
      }(new Port(port))
    }

  info("As a SBR user")
  info("I want to make an edit to the parent unit of a VAT record")
  info("So that the correct unit is shown as the parent")

  feature("edit an existing VAT records parent link") {
    scenario("by VAT reference (vatref) and UBRN for a specific period") { wsClient =>
      Given(s"a VAT record with $TargetVAT for $TargetPeriod has a unit link to a parent legal unit identified by $ParentLEU")
      And(s"And the parent unit link needs to be changed to the legal unit with LEU $NewParentLEU")
      stubSbrControlApiFor(aVatParentLinkEditRequest(withVatRef = TargetVAT, withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(VATEditParentLinkPatchBody))
        .willReturn(aNoContentResponse()))

      stubSbrControlApiFor(aLegalUnitChildLinkCreationRequest(withUbrn = UnitId(NewParentLEU), withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(LEUCreateChildLinkPatchBody))
        .willReturn(aNoContentResponse()))

      When(s"an edit request for the VAT unit with $TargetVAT is requested for $TargetPeriod")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/vats/${TargetVAT.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(VATEditParentLinkPostBody))

      Then(s"a Created response will be returned")
      response.status shouldBe CREATED
    }
  }

  feature("edit a non-existing VAT records parent link") {
    scenario("by VAT reference (vatref) and UBRN for a specific period") { wsClient =>
      Given(s"a VAT record with $TargetVAT for $TargetPeriod does not exist")
      stubSbrControlApiFor(aVatParentLinkEditRequest(withVatRef = TargetVAT, withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(VATEditParentLinkPatchBody))
        .willReturn(aNotFoundResponse()))

      When(s"an edit request for the VAT unit with $TargetVAT is requested for $TargetPeriod")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/vats/${TargetVAT.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(VATEditParentLinkPostBody))

      Then(s"a Not Found response will be returned")
      response.status shouldBe NOT_FOUND
    }
  }

  feature("edit an existing VAT records parent link, where the parent link cannot be found") {
    scenario("by VAT reference (vatref) and UBRN for a specific period") { wsClient =>
      Given(s"a VAT record with $TargetVAT for $TargetPeriod does not exist")
      And(s"And the parent unit link $ParentLEU cannot be found (resulting in a 422 response)")
      stubSbrControlApiFor(aVatParentLinkEditRequest(withVatRef = TargetVAT, withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(VATEditParentLinkPatchBody))
        .willReturn(anUnprocessableEntityResponse()))

      When(s"an edit request for the VAT unit with $TargetVAT is requested for $TargetPeriod")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/vats/${TargetVAT.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(VATEditParentLinkPostBody))

      Then(s"an Unprocessable response will be returned")
      response.status shouldBe UNPROCESSABLE_ENTITY
    }
  }

  feature("edit an existing VAT records parent link whilst another person is editing the same record") {
    scenario("by VAT reference (vatref) and UBRN for a specific period") { wsClient =>
      Given(s"a VAT record with $TargetVAT for $TargetPeriod exists")
      And(s"And the VAT record is also being edited by somebody else, resulting in a conflict")
      stubSbrControlApiFor(aVatParentLinkEditRequest(withVatRef = TargetVAT, withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(VATEditParentLinkPatchBody))
        .willReturn(aConflictResponse()))

      When(s"an edit request for the VAT unit with $TargetVAT is requested for $TargetPeriod")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/vats/${TargetVAT.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(VATEditParentLinkPostBody))

      Then(s"a Conflict response will be returned")
      response.status shouldBe CONFLICT
    }
  }

  feature("edit an existing VAT records parent link where general errors are occurring on sbr-control-api") {
    scenario("by VAT reference (vatref) and UBRN for a specific period") { wsClient =>
      Given(s"a VAT record with $TargetVAT for $TargetPeriod exists")
      stubSbrControlApiFor(aVatParentLinkEditRequest(withVatRef = TargetVAT, withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(VATEditParentLinkPatchBody))
        .willReturn(anInternalServerError()))

      When(s"an edit request for the VAT unit with $TargetVAT is requested for $TargetPeriod")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/vats/${TargetVAT.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(VATEditParentLinkPostBody))

      Then(s"an Internal Server Error response will be returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }
  }

  feature("edit an existing VAT records parent link using invalid JSON") {
    scenario("by VAT reference (vatref) and UBRN for a specific period") { wsClient =>
      Given(s"The edit JSON is invalid (extra closing brace)")
      When(s"an edit request for the VAT unit with $TargetVAT is requested for $TargetPeriod")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/vats/${TargetVAT.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(InvalidVATEditParentLinkPostBody))

      Then(s"a Bad Request response will be returned")
      response.status shouldBe BAD_REQUEST
    }
  }

  feature("edit an existing VAT records parent link using JSON that cannot be processed by sbr-control-api") {
    scenario("by VAT reference (vatref) and UBRN for a specific period") { wsClient =>
      Given(s"a valid edit request must contain from and to UBRN values")
      When(s"an edit request is made containing two from operations and no to operations")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/vats/${TargetVAT.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(UnprocessableVATEditParentLinkPostBody))

      Then(s"an Unprocessable response will be returned")
      response.status shouldBe UNPROCESSABLE_ENTITY
    }
  }

  feature("edit a non-existing LEU records child link") {
    scenario("by VAT reference (vatref) for a specific period") { wsClient =>
      Given(s"a VAT record with $TargetVAT for $TargetPeriod exists")
      stubSbrControlApiFor(aVatParentLinkEditRequest(withVatRef = TargetVAT, withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(VATEditParentLinkPatchBody))
        .willReturn(aNoContentResponse()))

      And(s"the requested LEU unit [$NewParentLEU] does not exist")
      stubSbrControlApiFor(aLegalUnitChildLinkCreationRequest(withUbrn = UnitId(NewParentLEU), withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(LEUCreateChildLinkPatchBody))
        .willReturn(aNotFoundResponse()))

      When(s"an edit request for the VAT unit with $TargetVAT is requested for $TargetPeriod")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/vats/${TargetVAT.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(VATEditParentLinkPostBody))

      Then(s"an Internal Server Error response will be returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }
  }

  feature("edit an LEU records child link whilst another person is editing it") {
    scenario("by VAT reference (vatref) for a specific period") { wsClient =>
      Given(s"a VAT record with $TargetVAT for $TargetPeriod exists")
      stubSbrControlApiFor(aVatParentLinkEditRequest(withVatRef = TargetVAT, withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(VATEditParentLinkPatchBody))
        .willReturn(aNoContentResponse()))

      And(s"the requested LEU unit [$NewParentLEU] is being edited by somebody else")
      stubSbrControlApiFor(aLegalUnitChildLinkCreationRequest(withUbrn = UnitId(NewParentLEU), withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(LEUCreateChildLinkPatchBody))
        .willReturn(aConflictResponse()))

      When(s"an edit request for the VAT unit with $TargetVAT is requested for $TargetPeriod")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/vats/${TargetVAT.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(VATEditParentLinkPostBody))

      Then(s"an Internal Server Error response will be returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }
  }

  feature("edit an LEU records child link with unprocessable JSON") {
    scenario("by VAT reference (vatref) for a specific period") { wsClient =>
      Given(s"a VAT record with $TargetVAT for $TargetPeriod exists")
      stubSbrControlApiFor(aVatParentLinkEditRequest(withVatRef = TargetVAT, withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(VATEditParentLinkPatchBody))
        .willReturn(aNoContentResponse()))

      And(s"the requested JSON for LEU unit [$NewParentLEU] is invalid")
      stubSbrControlApiFor(aLegalUnitChildLinkCreationRequest(withUbrn = UnitId(NewParentLEU), withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(LEUCreateChildLinkPatchBody))
        .willReturn(anUnprocessableEntityResponse()))

      When(s"an edit request for the VAT unit with $TargetVAT is requested for $TargetPeriod")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/vats/${TargetVAT.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(VATEditParentLinkPostBody))

      Then(s"an Internal Server Error response will be returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }
  }

  feature("edit an LEU records child link when general errors are occurring on sbr-control-api") {
    scenario("by VAT reference (vatref) for a specific period") { wsClient =>
      Given(s"a VAT record with $TargetVAT for $TargetPeriod exists")
      stubSbrControlApiFor(aVatParentLinkEditRequest(withVatRef = TargetVAT, withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(VATEditParentLinkPatchBody))
        .willReturn(aNoContentResponse()))

      And(s"the requested JSON for LEU unit [$NewParentLEU] is invalid")
      stubSbrControlApiFor(aLegalUnitChildLinkCreationRequest(withUbrn = UnitId(NewParentLEU), withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(LEUCreateChildLinkPatchBody))
        .willReturn(anInternalServerError()))

      When(s"an edit request for the VAT unit with $TargetVAT is requested for $TargetPeriod")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/vats/${TargetVAT.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(VATEditParentLinkPostBody))

      Then(s"an Internal Server Error response will be returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }
  }
}