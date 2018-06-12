import java.time.Month.MARCH

import fixture.ServerAcceptanceSpec
import org.scalatest.{ OptionValues, Outcome }
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.Port
import play.api.http.Status.{ BAD_REQUEST, GATEWAY_TIMEOUT, INTERNAL_SERVER_ERROR, NOT_FOUND, OK }
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.WsTestClient
import play.mvc.Http.MimeTypes.JSON
import support.matchers.HttpServerErrorStatusCodeMatcher
import support.matchers.HttpServerErrorStatusCodeMatcher.aServerError
import support.wiremock.{ WireMockAdminDataApi, WireMockSbrControlApi }
import uk.gov.ons.sbr.models._

class VatAcceptanceSpec extends ServerAcceptanceSpec with WireMockSbrControlApi with WireMockAdminDataApi with OptionValues {
  private val TargetVatRef = VatRef("397585634298")
  private val TargetPeriod = Period.fromYearMonth(2018, MARCH)

  private val VatUnitLinksResponseBody =
    s"""|{"id":"${TargetVatRef.value}",
        | "parents": {
        |   "ENT":"1000000012"
        | },
        | "unitType":"VAT",
        | "period":"${Period.asString(TargetPeriod)}"
        |}""".stripMargin

  private val VatUnitResponseBody =
    s"""|{"period":"${Period.asString(TargetPeriod)}",
        | "id":"${TargetVatRef.value}",
        | "variables": {
        |   "entref":"709076121581",
        |   "vatref":"${TargetVatRef.value}",
        |   "deathcode":"960896",
        |   "birthdate":"01/01/2016",
        |   "deathdate":"05/05/2015",
        |   "sic92":"12345",
        |   "turnover":360897416,
        |   "turnover_date":"07/07/2017",
        |   "record_type":"1",
        |   "legalstatus":"A",
        |   "actiondate":"01/01/2018",
        |   "crn":"6",
        |   "marker":"4",
        |   "addressref":"8321252441010",
        |   "inqcode":"361",
        |   "nameline1":"MOZ13T6P5B",
        |   "nameline2":"ASE300FH5Y",
        |   "nameline3":"91QF32WK4U",
        |   "tradstyle1":"WD54",
        |   "tradstyle2":"L2CS",
        |   "tradstyle3":"U45L",
        |   "address1":"K09R65FVF9ATRW25ET1RIPQDA",
        |   "address2":"IUFHGRV5",
        |   "address3":"0LZESXMOVZJ4",
        |   "address4":"NAX9GY3B4XJC",
        |   "address5":"6HK3M5XEM5F2",
        |   "postcode":"ZCD2 RFC"
        | }
        |}""".stripMargin

  private val ExpectedDetailsBody =
    s"""
       |{"id":"${TargetVatRef.value}",
       | "parents": {
       |   "ENT":"1000000012"
       | },
       | "unitType":"VAT",
       | "period":"${Period.asString(TargetPeriod)}",
       | "vars": {
       |   "entref":"709076121581",
       |   "vatref":"${TargetVatRef.value}",
       |   "deathcode":"960896",
       |   "birthdate":"01/01/2016",
       |   "deathdate":"05/05/2015",
       |   "sic92":"12345",
       |   "turnover":360897416,
       |   "turnover_date":"07/07/2017",
       |   "record_type":"1",
       |   "legalstatus":"A",
       |   "actiondate":"01/01/2018",
       |   "crn":"6",
       |   "marker":"4",
       |   "addressref":"8321252441010",
       |   "inqcode":"361",
       |   "nameline1":"MOZ13T6P5B",
       |   "nameline2":"ASE300FH5Y",
       |   "nameline3":"91QF32WK4U",
       |   "tradstyle1":"WD54",
       |   "tradstyle2":"L2CS",
       |   "tradstyle3":"U45L",
       |   "address1":"K09R65FVF9ATRW25ET1RIPQDA",
       |   "address2":"IUFHGRV5",
       |   "address3":"0LZESXMOVZJ4",
       |   "address4":"NAX9GY3B4XJC",
       |   "address5":"6HK3M5XEM5F2",
       |   "postcode":"ZCD2 RFC"
       | }
       |}"""".stripMargin

  override type FixtureParam = WSClient

  override protected def withFixture(test: OneArgTest): Outcome =
    withWireMockSbrControlApi { () =>
      withWireMockAdminDataApi { () =>
        WsTestClient.withClient { wsClient =>
          withFixture(test.toNoArgTest(wsClient))
        }(new Port(port))
      }
    }

  info("As a SBR user")
  info("I want to retrieve VAT unit details for a period in time")
  info("So that I can view VAT unit details via the user interface")

  feature("retrieve existing VAT unit details") {
    scenario("by VAT reference for a specific period") { wsClient =>
      Given(s"a unit link exists for a unit with $TargetVatRef for $TargetPeriod")
      stubSbrControlApiFor(aVatUnitLinksRequest(withVatRef = TargetVatRef, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(VatUnitLinksResponseBody)
      ))
      And(s"VAT admin data exists with $TargetVatRef for $TargetPeriod")
      stubAdminDataApiFor(aVatForPeriodRequest(withVatRef = TargetVatRef, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(VatUnitResponseBody)
      ))

      When(s"the VAT unit data with reference $TargetVatRef is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/vats/${TargetVatRef.value}").get())

      Then(s"the details of the VAT unit with $TargetVatRef for $TargetPeriod are returned")
      response.status shouldBe OK
      response.header(CONTENT_TYPE).value shouldBe JSON
      response.json shouldBe Json.parse(ExpectedDetailsBody)
    }
  }

  feature("retrieve non-existent VAT unit details") {
    scenario("when there is no VAT admin data with the specified VAT reference and period") { wsClient =>
      Given(s"a unit link exists for a unit with $TargetVatRef for $TargetPeriod")
      stubSbrControlApiFor(aVatUnitLinksRequest(withVatRef = TargetVatRef, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(VatUnitLinksResponseBody)
      ))
      And(s"VAT admin data does not exist with $TargetVatRef for $TargetPeriod")
      stubAdminDataApiFor(aVatForPeriodRequest(withVatRef = TargetVatRef, withPeriod = TargetPeriod).willReturn(
        aNotFoundResponse()
      ))

      When(s"the VAT unit data with reference $TargetVatRef is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/vats/${TargetVatRef.value}").get())

      Then("a NOT FOUND response is returned")
      response.status shouldBe NOT_FOUND
    }

    scenario("when there are no unit links defined for the specified VAT reference and period") { wsClient =>
      Given(s"a unit link does not exist for a unit with $TargetVatRef for $TargetPeriod")
      stubSbrControlApiFor(aVatUnitLinksRequest(withVatRef = TargetVatRef, withPeriod = TargetPeriod).willReturn(
        aNotFoundResponse()
      ))

      When(s"the VAT unit data with reference $TargetVatRef is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/vats/${TargetVatRef.value}").get())

      Then("a NOT FOUND response is returned")
      response.status shouldBe NOT_FOUND
    }
  }

  feature("handle failure gracefully") {
    scenario("when the retrieval of VAT admin data returns an error") { wsClient =>
      Given(s"a unit link exists for a unit with $TargetVatRef for $TargetPeriod")
      stubSbrControlApiFor(aVatUnitLinksRequest(withVatRef = TargetVatRef, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(VatUnitLinksResponseBody)
      ))
      And(s"retrieval of VAT admin data with $TargetVatRef for $TargetPeriod will return an error")
      stubAdminDataApiFor(aVatForPeriodRequest(withVatRef = TargetVatRef, withPeriod = TargetPeriod).willReturn(
        anInternalServerError()
      ))

      When(s"the VAT unit data with reference $TargetVatRef is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/vats/${TargetVatRef.value}").get())

      Then("an INTERNAL_SERVER_ERROR response is returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }

    scenario("when the retrieval of VAT unit links returns an error") { wsClient =>
      Given(s"retrieval of a unit with $TargetVatRef for $TargetPeriod will return an error")
      stubSbrControlApiFor(aVatUnitLinksRequest(withVatRef = TargetVatRef, withPeriod = TargetPeriod).willReturn(
        anInternalServerError()
      ))

      When(s"the VAT unit data with reference $TargetVatRef is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/vats/${TargetVatRef.value}").get())

      Then("an INTERNAL_SERVER_ERROR response is returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }

    scenario("the VAT admin data service is unavailable") { wsClient =>
      Given(s"a unit link exists for a unit with $TargetVatRef for $TargetPeriod")
      stubSbrControlApiFor(aVatUnitLinksRequest(withVatRef = TargetVatRef, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(VatUnitLinksResponseBody)
      ))
      And("the VAT admin data service is unavailable")
      stopMockAdminDataApi()

      When(s"the VAT unit data with reference $TargetVatRef is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/vats/${TargetVatRef.value}").get())

      Then("a server error is returned")
      response.status shouldBe aServerError
    }

    scenario("the sbr_control_api service is unavailable") { wsClient =>
      Given("the sbr_control_api service is unavailable")
      stopMockSbrControlApi()

      When(s"the VAT unit data with reference $TargetVatRef is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/vats/${TargetVatRef.value}").get())

      Then("a server error is returned")
      response.status shouldBe aServerError
    }
  }

  feature("validate request parameters") {
    scenario("rejecting a VAT reference that is too short") { wsClient =>
      Given("that a VAT reference is represented by a twelve digit number")

      When("the user requests a VAT unit with a reference less than twelve digits long")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/vats/123456789").get())

      Then("a BAD REQUEST response is returned")
      response.status shouldBe BAD_REQUEST
    }
  }
}
