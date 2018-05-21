import java.time.Month.MARCH

import fixture.ServerAcceptanceSpec
import org.scalatest.{ OptionValues, Outcome }
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.Port
import play.api.http.Status.{ BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, OK }
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.WsTestClient
import play.mvc.Http.MimeTypes.JSON
import support.wiremock.WireMockSbrControlApi
import uk.gov.ons.sbr.models._

class EnterpriseAcceptanceSpec extends ServerAcceptanceSpec with WireMockSbrControlApi with OptionValues {
  private val TargetErn = Ern("1000000012")
  private val TargetPeriod = Period.fromYearMonth(2018, MARCH)

  private val EnterpriseUnitLinksResponseBody =
    s"""
       |{"id":"${TargetErn.value}",
       | "children":{"10205415":"LEU","900000011":"LOU"},
       | "unitType":"ENT",
       | "period":"${Period.asString(TargetPeriod)}"
       |}""".stripMargin

  private val EnterpriseUnitResponseBody =
    s"""|{
        | "ern":"${TargetErn.value}",
        | "entref":"some-entref",
        | "name":"some-name",
        | "postcode":"some-postcode",
        | "legalStatus":"some-legalStatus",
        | "employees":42
        |}""".stripMargin

  private val ExpectedDetailsBody =
    s"""
       |{"id":"${TargetErn.value}",
       | "children": {
       |   "10205415":"LEU",
       |   "900000011":"LOU"
       | },
       | "unitType":"ENT",
       | "period":"${Period.asString(TargetPeriod)}",
       | "vars": {
       |   "ern":"${TargetErn.value}",
       |   "entref":"some-entref",
       |   "name":"some-name",
       |   "postcode":"some-postcode",
       |   "legalStatus":"some-legalStatus",
       |   "employees":42
       |  }
       |}"
     """.stripMargin

  override type FixtureParam = WSClient

  override protected def withFixture(test: OneArgTest): Outcome =
    withWireMockSbrControlApi { () =>
      WsTestClient.withClient { wsClient =>
        withFixture(test.toNoArgTest(wsClient))
      }(new Port(port))
    }

  info("As a SBR user")
  info("I want to retrieve an enterprise for a period in time")
  info("So that I can view the enterprise details via the user interface")

  feature("retrieve an existing Enterprise") {
    scenario("by Enterprise reference (ERN) for a specific period") { wsClient =>
      Given(s"an enterprise unit link exists for an enterprise with $TargetErn for $TargetPeriod")
      stubSbrControlApiFor(anEnterpriseUnitLinksRequest(withErn = TargetErn, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(EnterpriseUnitLinksResponseBody)
      ))
      And(s"an enterprise exists with $TargetErn for $TargetPeriod")
      stubSbrControlApiFor(anEnterpriseForPeriodRequest(withErn = TargetErn, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(EnterpriseUnitResponseBody)
      ))

      When(s"the enterprise with reference $TargetErn is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/ents/${TargetErn.value}").get())

      Then(s"the details of the enterprise with $TargetErn for $TargetPeriod are returned")
      response.status shouldBe OK
      response.header(CONTENT_TYPE).value shouldBe JSON
      response.json shouldBe Json.parse(ExpectedDetailsBody)
    }
  }

  feature("retrieve a non-existent Enterprise") {
    scenario("when there is no enterprise defined with the specified Enterprise reference (ERN) and period") { wsClient =>
      Given(s"an enterprise unit link exists for an enterprise with $TargetErn for $TargetPeriod")
      stubSbrControlApiFor(anEnterpriseUnitLinksRequest(withErn = TargetErn, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(EnterpriseUnitLinksResponseBody)
      ))
      And(s"an enterprise does not exist with $TargetErn for $TargetPeriod")
      stubSbrControlApiFor(anEnterpriseForPeriodRequest(withErn = TargetErn, withPeriod = TargetPeriod).willReturn(
        aNotFoundResponse()
      ))

      When(s"the enterprise with reference $TargetErn is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/ents/${TargetErn.value}").get())

      Then(s"a NOT FOUND response is returned")
      response.status shouldBe NOT_FOUND
    }

    scenario("when there are no unit links defined for the specified Enterprise reference (ERN) and period") { wsClient =>
      Given(s"an enterprise unit link does not exist for an enterprise with $TargetErn for $TargetPeriod")
      stubSbrControlApiFor(anEnterpriseUnitLinksRequest(withErn = TargetErn, withPeriod = TargetPeriod).willReturn(
        aNotFoundResponse()
      ))

      When(s"the enterprise with reference $TargetErn is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/ents/${TargetErn.value}").get())

      Then(s"a NOT FOUND response is returned")
      response.status shouldBe NOT_FOUND
    }
  }

  feature("validate request parameters") {
    scenario("rejecting an Enterprise reference (ERN) that is non-numeric") { wsClient =>
      Given("that an ERN is represented by a ten digit number")

      When("the user requests an Enterprise having an ERN that is non-numeric")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/ents/123456789A").get())

      Then("a BAD REQUEST response is returned")
      response.status shouldBe BAD_REQUEST
    }
  }

  feature("handle failure gracefully") {
    scenario("when the retrieval of an enterprise fails") { wsClient =>
      Given(s"an enterprise unit link exists for an enterprise with $TargetErn for $TargetPeriod")
      stubSbrControlApiFor(anEnterpriseUnitLinksRequest(withErn = TargetErn, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(EnterpriseUnitLinksResponseBody)
      ))
      And(s"the retrieval of an enterprise with $TargetErn for $TargetPeriod will fail")
      stubSbrControlApiFor(anEnterpriseForPeriodRequest(withErn = TargetErn, withPeriod = TargetPeriod).willReturn(
        anInternalServerError()
      ))

      When(s"the enterprise with reference $TargetErn is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/ents/${TargetErn.value}").get())

      Then(s"an INTERNAL_SERVER_ERROR response is returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }

    scenario("when the retrieval of enterprise unit links fails") { wsClient =>
      Given(s"the retrieval of enterprise unit links with $TargetErn for $TargetPeriod will fail")
      stubSbrControlApiFor(anEnterpriseUnitLinksRequest(withErn = TargetErn, withPeriod = TargetPeriod).willReturn(
        anInternalServerError()
      ))

      When(s"the enterprise with reference $TargetErn is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/ents/${TargetErn.value}").get())

      Then(s"an INTERNAL_SERVER_ERROR response is returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
