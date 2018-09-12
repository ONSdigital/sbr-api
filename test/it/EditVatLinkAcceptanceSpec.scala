import java.time.Month._

import com.github.tomakehurst.wiremock.client.ValueMatchingStrategy
import com.github.tomakehurst.wiremock.client.WireMock.{ equalTo, equalToJson }

import org.scalatest.{ OptionValues, Outcome }
import play.api.http.Status.CREATED
import play.api.libs.ws.WSClient
import play.api.test.WsTestClient
import play.api.test.Helpers._
import play.api.http.{ HeaderNames, Port }
import uk.gov.ons.sbr.models._
import fixture.ServerAcceptanceSpec
import support.wiremock.WireMockSbrControlApi

class EditVatLinkAcceptanceSpec extends ServerAcceptanceSpec with WireMockSbrControlApi with OptionValues {

  private val TargetVAT = VatRef("330000000511")
  private val ParentLEU = "1100000051"
  private val NewParentLEU = "1100000052"
  private val TargetPeriod = Period.fromYearMonth(2018, AUGUST)
  private val PatchJson = s"$JSON-patch+json"

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

  private val VATEditParentLinkPatchBody =
    s"""|[
        |  { "op": "test", path: "/parents/LEU", value: "$ParentLEU" },
        |  { "op": "replace", path: "/parents/LEU", value: "$NewParentLEU" }
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
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(PatchJson))
        .withRequestBody(equalToJson(VATEditParentLinkPatchBody))
        .willReturn(aNoContentResponse()))

      When(s"an edit request for the VAT unit with $TargetVAT is requested for $TargetPeriod")
      val response = await(wsClient
        .url(s"/v1/periods/${Period.asString(TargetPeriod)}/edit/vats/${TargetVAT.value}")
        .withHeaders((HeaderNames.CONTENT_TYPE, JSON))
        .post(VATEditParentLinkPostBody))

      Then(s"the status of the edit of the parent LEU from $ParentLEU to $NewParentLEU will be sent back")
      response.status shouldBe CREATED
    }
  }
}