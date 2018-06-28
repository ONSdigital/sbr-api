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
import uk.gov.ons.sbr.models.{ Ern, Lurn, Period }

class LocalUnitAcceptanceSpec extends ServerAcceptanceSpec with WireMockSbrControlApi with OptionValues {
  private val TargetLurn = Lurn("220000028")
  private val ParentErn = Ern("1100000052")
  private val TargetPeriod = Period.fromYearMonth(2018, MAY)

  private val LocalUnitUnitLinksResponseBody =
    s"""|{"id":"${TargetLurn.value}",
        | "parents": {
        |   "ENT":"${ParentErn.value}",
        |   "REU":"33000000052"
        | },
        | "unitType":"LOU",
        | "period":"${Period.asString(TargetPeriod)}"
        |}""".stripMargin

  private val LocalUnitResponseBody =
    s"""|{"lurn":"${TargetLurn.value}",
        | "luref":"50679152",
        | "enterprise": {
        |   "ern":"${ParentErn.value}",
        |   "entref":"9906044845"
        | },
        | "reportingUnit": {
        |   "rurn":"33000000052",
        |   "ruref":"49906044845"
        | },
        | "name":"Big Box Cereal",
        | "address": {
        |   "line1":"1 Blackbrook Farm Cottages",
        |   "line2":"Blackbrook Road",
        |   "line3":"Woodbridge",
        |   "line4":"Suffolk",
        |   "postcode":"WR2 6PG"
        | },
        | "sic07":"47710",
        | "employees":4
        |}""".stripMargin

  private val ExpectedDetailsBody =
    s"""|{"id":"${TargetLurn.value}",
        | "parents": {
        |   "ENT":"${ParentErn.value}",
        |   "REU":"33000000052"
        | },
        | "unitType":"LOU",
        | "period":"${Period.asString(TargetPeriod)}",
        | "vars": {
        |   "lurn":"${TargetLurn.value}",
        |   "luref":"50679152",
        |   "enterprise": {
        |     "ern":"${ParentErn.value}",
        |     "entref":"9906044845"
        |   },
        |   "reportingUnit": {
        |     "rurn":"33000000052",
        |     "ruref":"49906044845"
        |   },
        |   "name":"Big Box Cereal",
        |   "address": {
        |     "line1":"1 Blackbrook Farm Cottages",
        |     "line2":"Blackbrook Road",
        |     "line3":"Woodbridge",
        |     "line4":"Suffolk",
        |     "postcode":"WR2 6PG"
        |   },
        |   "sic07":"47710",
        |   "employees":4
        | }
        |}"""".stripMargin

  override type FixtureParam = WSClient

  override protected def withFixture(test: OneArgTest): Outcome =
    withWireMockSbrControlApi { () =>
      WsTestClient.withClient { wsClient =>
        withFixture(test.toNoArgTest(wsClient))
      }(new Port(port))
    }

  info("As a SBR user")
  info("I want to retrieve a local unit for a period in time")
  info("So that I can view the local unit details via the user interface")

  feature("retrieve an existing Local Unit") {
    scenario("by Local Unit reference (LURN) for a specific period") { wsClient =>
      Given(s"a local unit with $TargetLurn for $TargetPeriod has a unit link to a parent enterprise identified by $ParentErn")
      stubSbrControlApiFor(aLocalUnitUnitLinksRequest(withLurn = TargetLurn, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(LocalUnitUnitLinksResponseBody)
      ))
      And(s"a local unit exists with $ParentErn and $TargetLurn for $TargetPeriod")
      stubSbrControlApiFor(aLocalUnitForPeriodRequest(withErn = ParentErn, withLurn = TargetLurn, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(LocalUnitResponseBody)
      ))

      When(s"the local unit with $TargetLurn is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/lous/${TargetLurn.value}").get())

      Then(s"the details of the local unit with $ParentErn and $TargetLurn for $TargetPeriod are returned")
      response.status shouldBe OK
      response.header(CONTENT_TYPE).value shouldBe JSON
      response.json shouldBe Json.parse(ExpectedDetailsBody)
    }
  }

  feature("retrieve a non-existent Local Unit") {
    scenario("when there is no Local Unit defined with the specified Local Unit reference, parent Enterprise reference, and period") { wsClient =>
      Given(s"a local unit with $TargetLurn for $TargetPeriod has a unit link to a parent enterprise identified by $ParentErn")
      stubSbrControlApiFor(aLocalUnitUnitLinksRequest(withLurn = TargetLurn, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(LocalUnitUnitLinksResponseBody)
      ))
      And(s"a local unit does not exist with $ParentErn and $TargetLurn for $TargetPeriod")
      stubSbrControlApiFor(aLocalUnitForPeriodRequest(withErn = ParentErn, withLurn = TargetLurn, withPeriod = TargetPeriod).willReturn(
        aNotFoundResponse()
      ))

      When(s"the local unit with $TargetLurn is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/lous/${TargetLurn.value}").get())

      Then(s"a NOT FOUND response is returned")
      response.status shouldBe NOT_FOUND
    }

    scenario("when there are no unit links defined for the specified Local Unit reference (LURN) and period") { wsClient =>
      Given(s"a local unit with $TargetLurn for $TargetPeriod is without unit links")
      stubSbrControlApiFor(aLocalUnitUnitLinksRequest(withLurn = TargetLurn, withPeriod = TargetPeriod).willReturn(
        aNotFoundResponse()
      ))

      When(s"the local unit with $TargetLurn is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/lous/${TargetLurn.value}").get())

      Then(s"a NOT FOUND response is returned")
      response.status shouldBe NOT_FOUND
    }
  }

  feature("handle failure gracefully") {
    scenario("when the retrieval of a local unit fails") { wsClient =>
      Given(s"a local unit with $TargetLurn for $TargetPeriod has a unit link to a parent enterprise identified by $ParentErn")
      stubSbrControlApiFor(aLocalUnitUnitLinksRequest(withLurn = TargetLurn, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(LocalUnitUnitLinksResponseBody)
      ))
      And(s"the retrieval of a local unit with $ParentErn and $TargetLurn for $TargetPeriod will fail")
      stubSbrControlApiFor(aLocalUnitForPeriodRequest(withErn = ParentErn, withLurn = TargetLurn, withPeriod = TargetPeriod).willReturn(
        anInternalServerError()
      ))

      When(s"the local unit with $TargetLurn is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/lous/${TargetLurn.value}").get())

      Then(s"an INTERNAL_SERVER_ERROR response is returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }

    scenario("when the retrieval of unit links for a local unit fails") { wsClient =>
      Given(s"the retrieval of unit links for the local unit with $TargetLurn for $TargetPeriod will fail")
      stubSbrControlApiFor(aLocalUnitUnitLinksRequest(withLurn = TargetLurn, withPeriod = TargetPeriod).willReturn(
        anInternalServerError()
      ))

      When(s"the local unit with $TargetLurn is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/lous/${TargetLurn.value}").get())

      Then(s"an INTERNAL_SERVER_ERROR response is returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }

    scenario("the sbr_control_api service is unavailable") { wsClient =>
      Given("the sbr_control_api service is unavailable")
      stopMockSbrControlApi()

      When(s"the local unit with $TargetLurn is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/lous/${TargetLurn.value}").get())

      Then("a server error is returned")
      response.status shouldBe aServerError
    }
  }

  feature("validate request parameters") {
    scenario("rejecting a Local Unit reference that is non-numeric") { wsClient =>
      Given("that a Local Unit reference is represented by a nine digit number")

      When("the user requests a local unit with a reference that is non-numeric")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/lous/12345678Z").get())

      Then("a BAD REQUEST response is returned")
      response.status shouldBe BAD_REQUEST
    }
  }
}
