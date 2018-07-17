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
import support.wiremock.WireMockSbrControlApi
import uk.gov.ons.sbr.models.{ Ern, Period, Rurn }

class ReportingUnitAcceptanceSpec extends ServerAcceptanceSpec with WireMockSbrControlApi with OptionValues {
  private val TargetRurn = Rurn("33000000051")
  private val ParentErn = Ern("1100000051")
  private val TargetPeriod = Period.fromYearMonth(2018, MAY)

  private val ReportingUnitUnitLinksResponseBody =
    s"""|{
        | "id":"${TargetRurn.value}",
        | "parents": {
        |   "ENT":"${ParentErn.value}"
        | },
        | "children": {
        |   "123456789":"LOU"
        | },
        | "unitType":"REU",
        | "period":"${Period.asString(TargetPeriod)}"
        |}""".stripMargin

  private val ReportingUnitResponseBody =
    s"""|{
        | "rurn":"${TargetRurn.value}",
        | "ruref":"49906016135",
        | "ern":"${ParentErn.value}",
        | "entref":"9906016135",
        | "name":"Developments Ltd",
        | "legalStatus":"1",
        | "address1":"1 Borlases Cottages",
        | "address2":"Milley Road",
        | "address3":"Waltham St Lawrence",
        | "address4":"Reading",
        | "postcode":"BR3 1HG",
        | "sic07":"47710",
        | "employees":5,
        | "employment":5,
        | "turnover":369,
        | "prn":"0.158231512"
        |}""".stripMargin

  private val ExpectedDetailsBody =
    s"""|{
        | "id":"${TargetRurn.value}",
        | "parents": {
        |   "ENT":"${ParentErn.value}"
        | },
        | "children": {
        |   "123456789":"LOU"
        | },
        | "unitType":"REU",
        | "period":"${Period.asString(TargetPeriod)}",
        | "vars": {
        |   "rurn":"${TargetRurn.value}",
        |   "ruref":"49906016135",
        |   "ern":"${ParentErn.value}",
        |   "entref":"9906016135",
        |   "name":"Developments Ltd",
        |   "legalStatus":"1",
        |   "address1":"1 Borlases Cottages",
        |   "address2":"Milley Road",
        |   "address3":"Waltham St Lawrence",
        |   "address4":"Reading",
        |   "postcode":"BR3 1HG",
        |   "sic07":"47710",
        |   "employees":5,
        |   "employment":5,
        |   "turnover":369,
        |   "prn":"0.158231512"
        | }
        |}""".stripMargin

  override type FixtureParam = WSClient

  override protected def withFixture(test: OneArgTest): Outcome =
    withWireMockSbrControlApi { () =>
      WsTestClient.withClient { wsClient =>
        withFixture(test.toNoArgTest(wsClient))
      }(new Port(port))
    }

  info("As a SBR user")
  info("I want to retrieve a reporting unit for a period in time")
  info("So that I can view the reporting unit details via the user interface")

  feature("retrieve an existing Reporting Unit") {
    scenario("by Reporting Unit reference (RURN) for a specific period") { wsClient =>
      Given(s"a reporting unit with $TargetRurn for $TargetPeriod has a unit link to a parent enterprise identified by $ParentErn")
      stubSbrControlApiFor(aReportingUnitUnitLinksRequest(withRurn = TargetRurn, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(ReportingUnitUnitLinksResponseBody)
      ))
      And(s"a reporting unit exists with $ParentErn and $TargetRurn for $TargetPeriod")
      stubSbrControlApiFor(aReportingUnitForPeriodRequest(withErn = ParentErn, withRurn = TargetRurn, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(ReportingUnitResponseBody)
      ))

      When(s"the reporting unit with $TargetRurn is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/reus/${TargetRurn.value}").get())

      Then(s"the details of the reporting unit with $ParentErn and $TargetRurn for $TargetPeriod are returned")
      response.status shouldBe OK
      response.header(CONTENT_TYPE).value shouldBe JSON
      response.json shouldBe Json.parse(ExpectedDetailsBody)
    }
  }

  feature("retrieve a non-existent Reporting Unit") {
    scenario("when there is no Reporting Unit defined with the specified Reporting Unit reference, parent Enterprise reference, and period") { wsClient =>
      Given(s"a reporting unit with $TargetRurn for $TargetPeriod has a unit link to a parent enterprise identified by $ParentErn")
      stubSbrControlApiFor(aReportingUnitUnitLinksRequest(withRurn = TargetRurn, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(ReportingUnitUnitLinksResponseBody)
      ))
      And(s"a reporting unit does not exist with $ParentErn and $TargetRurn for $TargetPeriod")
      stubSbrControlApiFor(aReportingUnitForPeriodRequest(withErn = ParentErn, withRurn = TargetRurn, withPeriod = TargetPeriod).willReturn(
        aNotFoundResponse()
      ))

      When(s"the reporting unit with $TargetRurn is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/reus/${TargetRurn.value}").get())

      Then(s"a NOT FOUND response is returned")
      response.status shouldBe NOT_FOUND
    }

    scenario("when there are no unit links defined for the specified Reporting Unit reference (RURN) and period") { wsClient =>
      Given(s"a reporting unit with $TargetRurn for $TargetPeriod is without unit links")
      stubSbrControlApiFor(aReportingUnitUnitLinksRequest(withRurn = TargetRurn, withPeriod = TargetPeriod).willReturn(
        aNotFoundResponse()
      ))

      When(s"the reporting unit with $TargetRurn is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/reus/${TargetRurn.value}").get())

      Then(s"a NOT FOUND response is returned")
      response.status shouldBe NOT_FOUND
    }
  }

  feature("handle failure gracefully") {
    scenario("when the retrieval of a reporting unit fails") { wsClient =>
      Given(s"a reporting unit with $TargetRurn for $TargetPeriod has a unit link to a parent enterprise identified by $ParentErn")
      stubSbrControlApiFor(aReportingUnitUnitLinksRequest(withRurn = TargetRurn, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(ReportingUnitUnitLinksResponseBody)
      ))
      And(s"the retrieval of a reporting unit with $ParentErn and $TargetRurn for $TargetPeriod will fail")
      stubSbrControlApiFor(aReportingUnitForPeriodRequest(withErn = ParentErn, withRurn = TargetRurn, withPeriod = TargetPeriod).willReturn(
        anInternalServerError()
      ))

      When(s"the reporting unit with $TargetRurn is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/reus/${TargetRurn.value}").get())

      Then(s"an INTERNAL_SERVER_ERROR response is returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }

    scenario("when the retrieval of unit links for a reporting unit fails") { wsClient =>
      Given(s"the retrieval of unit links for the reporting unit with $TargetRurn for $TargetPeriod will fail")
      stubSbrControlApiFor(aReportingUnitUnitLinksRequest(withRurn = TargetRurn, withPeriod = TargetPeriod).willReturn(
        anInternalServerError()
      ))

      When(s"the reporting unit with $TargetRurn is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/reus/${TargetRurn.value}").get())

      Then(s"an INTERNAL_SERVER_ERROR response is returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }

    scenario("the sbr_control_api service is unavailable") { wsClient =>
      Given("the sbr_control_api service is unavailable")
      stopMockSbrControlApi()

      When(s"the reporting unit with $TargetRurn is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/reus/${TargetRurn.value}").get())

      Then("a server error is returned")
      response.status shouldBe aServerError
    }
  }

  feature("validate request parameters") {
    scenario("rejecting a Reporting Unit reference that is too short") { wsClient =>
      Given("that a Reporting Unit reference is represented by an eleven digit number")

      When("the user requests a reporting unit with a reference that has less than eleven digits")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/reus/1234567890").get())

      Then("a BAD REQUEST response is returned")
      response.status shouldBe BAD_REQUEST
    }
  }
}
