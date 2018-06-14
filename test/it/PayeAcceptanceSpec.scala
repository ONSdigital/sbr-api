import java.time.Month.MAY

import fixture.ServerAcceptanceSpec
import org.scalatest.{ OptionValues, Outcome }
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.Port
import play.api.http.Status.{ BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, OK }
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.WsTestClient
import play.mvc.Http.MimeTypes.JSON
import support.matchers.HttpServerErrorStatusCodeMatcher.aServerError
import support.wiremock.{ WireMockAdminDataApi, WireMockSbrControlApi }
import uk.gov.ons.sbr.models.{ PayeRef, Period }

class PayeAcceptanceSpec extends ServerAcceptanceSpec with WireMockSbrControlApi with WireMockAdminDataApi with OptionValues {
  private val TargetPayeRef = PayeRef("065H7Z31732")
  private val TargetPeriod = Period.fromYearMonth(2018, MAY)

  private val PayeUnitLinksResponseBody =
    s"""|{"id":"${TargetPayeRef.value}",
        | "parents": {
        |   "LEU":"1111111110123456"
        | },
        | "unitType":"PAYE",
        | "period":"${Period.asString(TargetPeriod)}"
        |}""".stripMargin

  private val PayeUnitResponseBody =
    s"""|{"period":"${Period.asString(TargetPeriod)}",
        | "id":"${TargetPayeRef.value}",
        | "variables": {
        |   "entref":"5235981614",
        |   "payeref":"${TargetPayeRef.value}",
        |   "deathcode":"658664",
        |   "birthdate":"01/01/2016",
        |   "deathdate":"05/05/2015",
        |   "mfullemp":9,
        |   "msubemp":5,
        |   "ffullemp":8,
        |   "fsubemp":2,
        |   "unclemp":3,
        |   "unclsubemp":5,
        |   "dec_jobs":6,
        |   "mar_jobs":1,
        |   "june_jobs":9,
        |   "sept_jobs":9,
        |   "jobs_lastupd":"01/01/2018",
        |   "legalstatus":"A",
        |   "prevpaye":"2",
        |   "employer_cat":"9",
        |   "stc":"6616",
        |   "crn":"1",
        |   "actiondate":"01/02/2018",
        |   "addressref":"9607",
        |   "marker":"1",
        |   "inqcode":"OR6PHFQ78Q",
        |   "nameline1":"VDEPJ0IVE5",
        |   "nameline2":"8JOS45YC8U",
        |   "nameline3":"IEENIUFNHI",
        |   "tradstyle1":"WD45",
        |   "tradstyle2":"L3CS",
        |   "tradstyle3":"U54L",
        |   "address1":"VFHLNA0MSJ",
        |   "address2":"P4FUV3QM7D",
        |   "address3":"5TM1RA3CFR",
        |   "address4":"00N7E1PVVM",
        |   "address5":"HKJY8TOMJ8",
        |   "postcode":"K6ZL 4GL"
        | }
        |}""".stripMargin

  private val ExpectedDetailsBody =
    s"""
       |{"id":"${TargetPayeRef.value}",
       | "parents": {
       |   "LEU":"1111111110123456"
       | },
       | "unitType":"PAYE",
       | "period":"${Period.asString(TargetPeriod)}",
       | "vars": {
       |   "entref":"5235981614",
       |   "payeref":"${TargetPayeRef.value}",
       |   "deathcode":"658664",
       |   "birthdate":"01/01/2016",
       |   "deathdate":"05/05/2015",
       |   "mfullemp":9,
       |   "msubemp":5,
       |   "ffullemp":8,
       |   "fsubemp":2,
       |   "unclemp":3,
       |   "unclsubemp":5,
       |   "dec_jobs":6,
       |   "mar_jobs":1,
       |   "june_jobs":9,
       |   "sept_jobs":9,
       |   "jobs_lastupd":"01/01/2018",
       |   "legalstatus":"A",
       |   "prevpaye":"2",
       |   "employer_cat":"9",
       |   "stc":"6616",
       |   "crn":"1",
       |   "actiondate":"01/02/2018",
       |   "addressref":"9607",
       |   "marker":"1",
       |   "inqcode":"OR6PHFQ78Q",
       |   "nameline1":"VDEPJ0IVE5",
       |   "nameline2":"8JOS45YC8U",
       |   "nameline3":"IEENIUFNHI",
       |   "tradstyle1":"WD45",
       |   "tradstyle2":"L3CS",
       |   "tradstyle3":"U54L",
       |   "address1":"VFHLNA0MSJ",
       |   "address2":"P4FUV3QM7D",
       |   "address3":"5TM1RA3CFR",
       |   "address4":"00N7E1PVVM",
       |   "address5":"HKJY8TOMJ8",
       |   "postcode":"K6ZL 4GL"
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
  info("I want to retrieve PAYE unit details for a period in time")
  info("So that I can view PAYE unit details via the user interface")

  feature("retrieve existing PAYE unit details") {
    scenario("by PAYE reference for a specific period") { wsClient =>
      Given(s"a unit link exists for a unit with $TargetPayeRef for $TargetPeriod")
      stubSbrControlApiFor(aPayeUnitLinksRequest(withPayeRef = TargetPayeRef, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(PayeUnitLinksResponseBody)
      ))
      And(s"PAYE admin data exists with $TargetPayeRef for $TargetPeriod")
      stubAdminDataApiFor(aPayeForPeriodRequest(withPayeRef = TargetPayeRef, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(PayeUnitResponseBody)
      ))

      When(s"the PAYE unit data with reference $TargetPayeRef is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/payes/${TargetPayeRef.value}").get())

      Then(s"the details of the PAYE unit with $TargetPayeRef for $TargetPeriod are returned")
      response.status shouldBe OK
      response.header(CONTENT_TYPE).value shouldBe JSON
      response.json shouldBe Json.parse(ExpectedDetailsBody)
    }
  }

  feature("retrieve non-existent PAYE unit details") {
    scenario("when there is no PAYE admin data with the specified PAYE reference and period") { wsClient =>
      Given(s"a unit link exists for a unit with $TargetPayeRef for $TargetPeriod")
      stubSbrControlApiFor(aPayeUnitLinksRequest(withPayeRef = TargetPayeRef, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(PayeUnitLinksResponseBody)
      ))
      And(s"PAYE admin data does not exist with $TargetPayeRef for $TargetPeriod")
      stubAdminDataApiFor(aPayeForPeriodRequest(withPayeRef = TargetPayeRef, withPeriod = TargetPeriod).willReturn(
        aNotFoundResponse()
      ))

      When(s"the PAYE unit data with reference $TargetPayeRef is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/payes/${TargetPayeRef.value}").get())

      Then("a NOT FOUND response is returned")
      response.status shouldBe NOT_FOUND
    }

    scenario("when there are no unit links defined for the specified PAYE reference and period") { wsClient =>
      Given(s"a unit link does not exist for a unit with $TargetPayeRef for $TargetPeriod")
      stubSbrControlApiFor(aPayeUnitLinksRequest(withPayeRef = TargetPayeRef, withPeriod = TargetPeriod).willReturn(
        aNotFoundResponse()
      ))

      When(s"the PAYE unit data with reference $TargetPayeRef is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/payes/${TargetPayeRef.value}").get())

      Then("a NOT FOUND response is returned")
      response.status shouldBe NOT_FOUND
    }
  }

  feature("handle failure gracefully") {
    scenario("when the retrieval of PAYE admin data returns an error") { wsClient =>
      Given(s"a unit link exists for a unit with $TargetPayeRef for $TargetPeriod")
      stubSbrControlApiFor(aPayeUnitLinksRequest(withPayeRef = TargetPayeRef, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(PayeUnitLinksResponseBody)
      ))
      And(s"retrieval of PAYE admin data with $TargetPayeRef for $TargetPeriod will return an error")
      stubAdminDataApiFor(aPayeForPeriodRequest(withPayeRef = TargetPayeRef, withPeriod = TargetPeriod).willReturn(
        anInternalServerError()
      ))

      When(s"the PAYE unit data with reference $TargetPayeRef is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/payes/${TargetPayeRef.value}").get())

      Then("an INTERNAL_SERVER_ERROR response is returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }

    scenario("when the retrieval of PAYE unit links returns an error") { wsClient =>
      Given(s"retrieval of a unit with $TargetPayeRef for $TargetPeriod will return an error")
      stubSbrControlApiFor(aPayeUnitLinksRequest(withPayeRef = TargetPayeRef, withPeriod = TargetPeriod).willReturn(
        anInternalServerError()
      ))

      When(s"the PAYE unit data with reference $TargetPayeRef is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/payes/${TargetPayeRef.value}").get())

      Then("an INTERNAL_SERVER_ERROR response is returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }

    scenario("the PAYE admin data service is unavailable") { wsClient =>
      Given(s"a unit link exists for a unit with $TargetPayeRef for $TargetPeriod")
      stubSbrControlApiFor(aPayeUnitLinksRequest(withPayeRef = TargetPayeRef, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(PayeUnitLinksResponseBody)
      ))
      And("the PAYE admin data service is unavailable")
      stopMockAdminDataApi()

      When(s"the PAYE unit data with reference $TargetPayeRef is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/payes/${TargetPayeRef.value}").get())

      Then("a server error is returned")
      response.status shouldBe aServerError
    }

    scenario("the sbr_control_api service is unavailable") { wsClient =>
      Given("the sbr_control_api service is unavailable")
      stopMockSbrControlApi()

      When(s"the PAYE unit data with reference $TargetPayeRef is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/payes/${TargetPayeRef.value}").get())

      Then("a server error is returned")
      response.status shouldBe aServerError
    }
  }

  feature("validate request parameters") {
    scenario("rejecting a period that is non-numeric") { wsClient =>
      Given("that a period is represented in YYYYMM format")

      When("the user requests a PAYE unit with a non-numeric period")
      val response = await(wsClient.url(s"/v1/periods/may-2018/payes/${TargetPayeRef.value}").get())

      Then("a BAD REQUEST response is returned")
      response.status shouldBe BAD_REQUEST
    }
  }
}
