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

class EditPayeParentLinkAcceptanceSpec extends ServerAcceptanceSpec with WireMockSbrControlApi with OptionValues {

  private val TargetPAYE = PayeRef("6574839200")
  private val ParentLEU = "1100000051"
  private val NewParentLEU = "1100000052"
  private val TargetPeriod = Period.fromYearMonth(2018, AUGUST)

  private val EditParentLinkPostBody =
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

  private val InvalidEditParentLinkPostBody = EditParentLinkPostBody + "}"

  private val UnprocessableEditParentLinkPostBody = EditParentLinkPostBody.replace("to", "from")

  private val EditParentLinkPatchBody =
    s"""|[
        |  { "op": "test", path: "/parents/LEU", value: "$ParentLEU" },
        |  { "op": "replace", path: "/parents/LEU", value: "$NewParentLEU" }
        |]""".stripMargin

  private val EditParentLinkConflictPatchBody =
    s"""|[
        |  { "op": "test", path: "/parents/LEU", value: "$NewParentLEU" },
        |  { "op": "replace", path: "/parents/LEU", value: "$NewParentLEU" }
        |]""".stripMargin

  private val PayeCreateChildLinkPatchBody =
    s"""|[
        |  { "op": "add", path: "/children/${TargetPAYE.value}", value: "PAYE" }
        |]""".stripMargin

  private val PayeDeleteChildUnitLink =
    s"""|[
        |  { "op": "test", path: "/children/${TargetPAYE.value}", value: "PAYE" },
        |  { "op": "remove", path: "/children/${TargetPAYE.value}" }
        |]""".stripMargin

  override type FixtureParam = WSClient

  override protected def withFixture(test: OneArgTest): Outcome =
    withWireMockSbrControlApi { () =>
      WsTestClient.withClient { wsClient =>
        withFixture(test.toNoArgTest(wsClient))
      }(new Port(port))
    }

  info("As a SBR user")
  info("I want to make an edit to the parent unit of a PAYE record")
  info("So that the correct unit is shown as the parent")

  feature("edit an existing PAYE records parent link") {
    scenario("by PAYE reference (payeref) and UBRN for a specific period") { wsClient =>
      Given(s"a PAYE record with $TargetPAYE for $TargetPeriod has a unit link to a parent legal unit identified by $ParentLEU")
      And(s"And the parent unit link needs to be changed to the legal unit with LEU $NewParentLEU")
      stubSbrControlApiFor(aPayeParentLinkEditRequest(withPayeRef = TargetPAYE, withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(EditParentLinkPatchBody))
        .willReturn(aNoContentResponse()))

      And(s"the request to add the new PAYE child link [$TargetPAYE] succeeds")
      stubSbrControlApiFor(aLegalUnitEditRequest(withUbrn = UnitId(NewParentLEU), withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(PayeCreateChildLinkPatchBody))
        .willReturn(aNoContentResponse()))

      And(s"the request to remove the LEU child link [$TargetPAYE] succeeds")
      stubSbrControlApiFor(aLegalUnitEditRequest(withUbrn = UnitId(ParentLEU), withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(PayeDeleteChildUnitLink))
        .willReturn(aNoContentResponse()))

      When(s"an edit request for the PAYE unit with $TargetPAYE is requested for $TargetPeriod")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/payes/${TargetPAYE.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(EditParentLinkPostBody))

      Then(s"a Created response will be returned")
      response.status shouldBe CREATED
    }
  }

  feature("edit a non-existing PAYE records parent link") {
    scenario("by PAYE reference (payeref) and UBRN for a specific period") { wsClient =>
      Given(s"a PAYE record with $TargetPAYE for $TargetPeriod does not exist")
      stubSbrControlApiFor(aPayeParentLinkEditRequest(withPayeRef = TargetPAYE, withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(EditParentLinkPatchBody))
        .willReturn(aNotFoundResponse()))

      When(s"an edit request for the PAYE unit with $TargetPAYE is requested for $TargetPeriod")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/payes/${TargetPAYE.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(EditParentLinkPostBody))

      Then(s"a Not Found response will be returned")
      response.status shouldBe NOT_FOUND
    }
  }

  feature("edit an existing PAYE records parent link, where the parent link cannot be found") {
    scenario("by PAYE reference (payeref) and UBRN for a specific period") { wsClient =>
      Given(s"a PAYE record with $TargetPAYE for $TargetPeriod exists")
      And(s"And the parent unit link $ParentLEU cannot be found (resulting in a 422 response)")
      stubSbrControlApiFor(aPayeParentLinkEditRequest(withPayeRef = TargetPAYE, withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(EditParentLinkPatchBody))
        .willReturn(anUnprocessableEntityResponse()))

      When(s"an edit request for the PAYE unit with $TargetPAYE is requested for $TargetPeriod")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/payes/${TargetPAYE.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(EditParentLinkPostBody))

      Then(s"an Unprocessable response will be returned")
      response.status shouldBe UNPROCESSABLE_ENTITY
    }
  }

  feature("edit an existing PAYE records parent link whilst another person is editing the same record") {
    scenario("by PAYE reference (payeref) and UBRN for a specific period") { wsClient =>
      Given(s"a PAYE record with $TargetPAYE for $TargetPeriod exists")
      And(s"And the PAYE record is also being edited by somebody else, resulting in a conflict")
      stubSbrControlApiFor(aPayeParentLinkEditRequest(withPayeRef = TargetPAYE, withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(EditParentLinkPatchBody))
        .willReturn(aConflictResponse()))

      And(s"And an update request using the same from/to $NewParentLEU value is submitted, to confirm the conflict")
      And("as an actual conflict has occurred, a Conflict status is returned")
      stubSbrControlApiFor(aPayeParentLinkEditRequest(withPayeRef = TargetPAYE, withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(EditParentLinkConflictPatchBody))
        .willReturn(aConflictResponse()))

      When(s"an edit request for the PAYE unit with $TargetPAYE is requested for $TargetPeriod")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/payes/${TargetPAYE.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(EditParentLinkPostBody))

      Then(s"a Conflict response will be returned")
      response.status shouldBe CONFLICT
    }
  }

  feature("retry a request (that previously failed) where only the first update operation succeeded") {
    scenario("by PAYE reference (payeref) and UBRN for a specific period") { wsClient =>
      Given(s"a PAYE record with $TargetPAYE for $TargetPeriod exists")
      And("And the UBRN from value is invalid (as the previous update operation succeeded), resulting in a Conflict")
      stubSbrControlApiFor(aPayeParentLinkEditRequest(withPayeRef = TargetPAYE, withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(EditParentLinkPatchBody))
        .willReturn(aConflictResponse()))

      And(s"And an update request using the same from/to $NewParentLEU value is submitted, to confirm the conflict")
      And("as the from/to values match whats in the database, a NoContentResponse is returned")
      stubSbrControlApiFor(aPayeParentLinkEditRequest(withPayeRef = TargetPAYE, withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(EditParentLinkConflictPatchBody))
        .willReturn(aNoContentResponse()))

      And(s"the request to add the new PAYE child link [$TargetPAYE] succeeds")
      stubSbrControlApiFor(aLegalUnitEditRequest(withUbrn = UnitId(NewParentLEU), withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(PayeCreateChildLinkPatchBody))
        .willReturn(aNoContentResponse()))

      And(s"the request to remove the LEU child link [$TargetPAYE] succeeds")
      stubSbrControlApiFor(aLegalUnitEditRequest(withUbrn = UnitId(ParentLEU), withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(PayeDeleteChildUnitLink))
        .willReturn(aNoContentResponse()))

      When(s"an edit request for the PAYE unit with $TargetPAYE is requested for $TargetPeriod")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/payes/${TargetPAYE.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(EditParentLinkPostBody))

      Then(s"a Created response will be returned")
      response.status shouldBe CREATED
    }
  }

  feature("edit an existing PAYE records parent link where general errors are occurring on sbr-control-api") {
    scenario("by PAYE reference (payeref) and UBRN for a specific period") { wsClient =>
      Given(s"a PAYE record with $TargetPAYE for $TargetPeriod exists")
      stubSbrControlApiFor(aPayeParentLinkEditRequest(withPayeRef = TargetPAYE, withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(EditParentLinkPatchBody))
        .willReturn(anInternalServerError()))

      When(s"an edit request for the PAYE unit with $TargetPAYE is requested for $TargetPeriod")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/payes/${TargetPAYE.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(EditParentLinkPostBody))

      Then(s"an Internal Server Error response will be returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }
  }

  feature("edit an existing PAYE records parent link using invalid JSON") {
    scenario("by PAYE reference (payeref) and UBRN for a specific period") { wsClient =>
      Given(s"The edit JSON is invalid (extra closing brace)")
      When(s"an edit request for the PAYE unit with $TargetPAYE is requested for $TargetPeriod")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/payes/${TargetPAYE.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(InvalidEditParentLinkPostBody))

      Then(s"a Bad Request response will be returned")
      response.status shouldBe BAD_REQUEST
    }
  }

  feature("edit an existing PAYE records parent link using JSON that cannot be processed by sbr-control-api") {
    scenario("by PAYE reference (payeref) and UBRN for a specific period") { wsClient =>
      Given(s"a valid edit request must contain from and to UBRN values")
      When(s"an edit request is made containing two from operations and no to operations")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/payes/${TargetPAYE.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(UnprocessableEditParentLinkPostBody))

      Then(s"an Unprocessable response will be returned")
      response.status shouldBe UNPROCESSABLE_ENTITY
    }
  }

  feature("edit a non-existing LEU records child link") {
    scenario("by PAYE reference (payeref) for a specific period") { wsClient =>
      Given(s"a PAYE record with $TargetPAYE for $TargetPeriod exists")
      stubSbrControlApiFor(aPayeParentLinkEditRequest(withPayeRef = TargetPAYE, withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(EditParentLinkPatchBody))
        .willReturn(aNoContentResponse()))

      And(s"the request for LEU unit [$ParentLEU] succeeds")
      stubSbrControlApiFor(aLegalUnitEditRequest(withUbrn = UnitId(ParentLEU), withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(PayeDeleteChildUnitLink))
        .willReturn(aNoContentResponse()))

      And(s"the requested LEU unit [$NewParentLEU] does not exist")
      stubSbrControlApiFor(aLegalUnitEditRequest(withUbrn = UnitId(NewParentLEU), withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(PayeCreateChildLinkPatchBody))
        .willReturn(aNotFoundResponse()))

      When(s"an edit request for the PAYE unit with $TargetPAYE is requested for $TargetPeriod")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/payes/${TargetPAYE.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(EditParentLinkPostBody))

      Then(s"an Internal Server Error response will be returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }
  }

  feature("edit an LEU records child link with unprocessable JSON") {
    scenario("by PAYE reference (payeref) for a specific period") { wsClient =>
      Given(s"a PAYE record with $TargetPAYE for $TargetPeriod exists")
      stubSbrControlApiFor(aPayeParentLinkEditRequest(withPayeRef = TargetPAYE, withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(EditParentLinkPatchBody))
        .willReturn(aNoContentResponse()))

      And(s"the request for LEU unit [$ParentLEU] succeeds")
      stubSbrControlApiFor(aLegalUnitEditRequest(withUbrn = UnitId(ParentLEU), withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(PayeDeleteChildUnitLink))
        .willReturn(aNoContentResponse()))

      And(s"the request for LEU unit [$NewParentLEU] is unprocessable (cannot be found)")
      stubSbrControlApiFor(aLegalUnitEditRequest(withUbrn = UnitId(NewParentLEU), withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(PayeCreateChildLinkPatchBody))
        .willReturn(anUnprocessableEntityResponse()))

      When(s"an edit request for the PAYE unit with $TargetPAYE is requested for $TargetPeriod")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/payes/${TargetPAYE.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(EditParentLinkPostBody))

      Then(s"an Internal Server Error response will be returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }
  }

  feature("edit an LEU records child link when general errors are occurring on sbr-control-api") {
    scenario("by PAYE reference (payeref) for a specific period") { wsClient =>
      Given(s"a PAYE record with $TargetPAYE for $TargetPeriod exists")
      stubSbrControlApiFor(aPayeParentLinkEditRequest(withPayeRef = TargetPAYE, withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(EditParentLinkPatchBody))
        .willReturn(aNoContentResponse()))

      And(s"the request for LEU unit [$ParentLEU] fails due to general errors on sbr-control-api")
      stubSbrControlApiFor(aLegalUnitEditRequest(withUbrn = UnitId(ParentLEU), withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(PayeDeleteChildUnitLink))
        .willReturn(anInternalServerError()))

      And(s"the request for LEU unit [$NewParentLEU] fails due to general errors on sbr-control-api")
      stubSbrControlApiFor(aLegalUnitEditRequest(withUbrn = UnitId(NewParentLEU), withPeriod = TargetPeriod)
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(JsonPatchMediaType))
        .withRequestBody(equalToJson(PayeCreateChildLinkPatchBody))
        .willReturn(anInternalServerError()))

      When(s"an edit request for the PAYE unit with $TargetPAYE is requested for $TargetPeriod")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/payes/${TargetPAYE.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(EditParentLinkPostBody))

      Then(s"an Internal Server Error response will be returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
