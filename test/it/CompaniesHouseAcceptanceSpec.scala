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
import uk.gov.ons.sbr.models.{ CompanyRefNumber, Period }

class CompaniesHouseAcceptanceSpec extends ServerAcceptanceSpec with WireMockSbrControlApi with WireMockAdminDataApi with OptionValues {
  private val TargetCompanyRefNumber = CompanyRefNumber("03007252")
  private val TargetPeriod = Period.fromYearMonth(2018, MAY)

  private val CompaniesHouseUnitLinksResponseBody =
    s"""|{"id":"${TargetCompanyRefNumber.value}",
        | "parents": {
        |   "LEU":"1111111110123456"
        | },
        | "unitType":"CH",
        | "period":"${Period.asString(TargetPeriod)}"
        |}""".stripMargin

  private val CompaniesHouseUnitResponseBody =
    s"""|{"period":"${Period.asString(TargetPeriod)}",
        | "id":"${TargetCompanyRefNumber.value}",
        | "variables": {
        |   "companyname":"HOME AND LEGACY INSURANCE SERVICES LIMITED",
        |   "companynumber":"${TargetCompanyRefNumber.value}",
        |   "regaddress_careof":"",
        |   "regaddress_pobox":"",
        |   "regaddress_addressline1":"57 LADYMEAD",
        |   "regaddress_addressline2":"",
        |   "regaddress_posttown":"GUILDFORD",
        |   "regaddress_county":"SURREY",
        |   "regaddress_country":"",
        |   "regaddress_postcode":"GU1 1DB",
        |   "companycategory":"Private Limited Company",
        |   "companystatus":"Active",
        |   "countryoforigin":"United Kingdom",
        |   "dissolutiondate":"",
        |   "incorporationdate":"09/01/1995",
        |   "accounts_accountrefday":"31",
        |   "accounts_accountrefmonth":"12",
        |   "accounts_nextduedate":"30/09/2018",
        |   "accounts_lastmadeupdate":"31/12/2016",
        |   "accounts_accountcategory":"FULL",
        |   "returns_nextduedate":"06/02/2017",
        |   "returns_lastmadeupdate":"09/01/2016",
        |   "mortgages_nummortcharges":"6",
        |   "mortgages_nummortoutstanding":"0",
        |   "mortgages_nummortpartsatisfied":"0",
        |   "mortgages_nummortsatisfied":"6",
        |   "siccode_sictext_1":"66220 - Activities of insurance agents and brokers",
        |   "siccode_sictext_2":"",
        |   "siccode_sictext_3":"",
        |   "siccode_sictext_4":"",
        |   "limitedpartnerships_numgenpartners":"0",
        |   "limitedpartnerships_numlimpartners":"0",
        |   "uri":"http://business.data.gov.uk/id/company/03007252",
        |   "previousname_1_condate":"",
        |   "previousname_1_companyname":"",
        |   "previousname_2_condate":"",
        |   "previousname_2_companyname":"",
        |   "previousname_3_condate":"",
        |   "previousname_3_companyname":"",
        |   "previousname_4_condate":"",
        |   "previousname_4_companyname":"",
        |   "previousname_5_condate":"",
        |   "previousname_5_companyname":"",
        |   "previousname_6_condate":"",
        |   "previousname_6_companyname":"",
        |   "previousname_7_condate":"",
        |   "previousname_7_companyname":"",
        |   "previousname_8_condate":"",
        |   "previousname_8_companyname":"",
        |   "previousname_9_condate":"",
        |   "previousname_9_companyname":"",
        |   "previousname_10_condate":"",
        |   "previousname_10_companyname":"",
        |   "confstmtnextduedate":"24/01/2018",
        |   "confstmtlastmadeupdate":"10/01/2017",
        |   "ref_period":"201706"
        | }
        |}""".stripMargin

  private val ExpectedDetailsBody =
    s"""
       |{"id":"${TargetCompanyRefNumber.value}",
       | "parents": {
       |   "LEU":"1111111110123456"
       | },
       | "unitType":"CH",
       | "period":"${Period.asString(TargetPeriod)}",
       | "vars": {
       |   "companyname":"HOME AND LEGACY INSURANCE SERVICES LIMITED",
       |   "companynumber":"${TargetCompanyRefNumber.value}",
       |   "regaddress_careof":"",
       |   "regaddress_pobox":"",
       |   "regaddress_addressline1":"57 LADYMEAD",
       |   "regaddress_addressline2":"",
       |   "regaddress_posttown":"GUILDFORD",
       |   "regaddress_county":"SURREY",
       |   "regaddress_country":"",
       |   "regaddress_postcode":"GU1 1DB",
       |   "companycategory":"Private Limited Company",
       |   "companystatus":"Active",
       |   "countryoforigin":"United Kingdom",
       |   "dissolutiondate":"",
       |   "incorporationdate":"09/01/1995",
       |   "accounts_accountrefday":"31",
       |   "accounts_accountrefmonth":"12",
       |   "accounts_nextduedate":"30/09/2018",
       |   "accounts_lastmadeupdate":"31/12/2016",
       |   "accounts_accountcategory":"FULL",
       |   "returns_nextduedate":"06/02/2017",
       |   "returns_lastmadeupdate":"09/01/2016",
       |   "mortgages_nummortcharges":"6",
       |   "mortgages_nummortoutstanding":"0",
       |   "mortgages_nummortpartsatisfied":"0",
       |   "mortgages_nummortsatisfied":"6",
       |   "siccode_sictext_1":"66220 - Activities of insurance agents and brokers",
       |   "siccode_sictext_2":"",
       |   "siccode_sictext_3":"",
       |   "siccode_sictext_4":"",
       |   "limitedpartnerships_numgenpartners":"0",
       |   "limitedpartnerships_numlimpartners":"0",
       |   "uri":"http://business.data.gov.uk/id/company/03007252",
       |   "previousname_1_condate":"",
       |   "previousname_1_companyname":"",
       |   "previousname_2_condate":"",
       |   "previousname_2_companyname":"",
       |   "previousname_3_condate":"",
       |   "previousname_3_companyname":"",
       |   "previousname_4_condate":"",
       |   "previousname_4_companyname":"",
       |   "previousname_5_condate":"",
       |   "previousname_5_companyname":"",
       |   "previousname_6_condate":"",
       |   "previousname_6_companyname":"",
       |   "previousname_7_condate":"",
       |   "previousname_7_companyname":"",
       |   "previousname_8_condate":"",
       |   "previousname_8_companyname":"",
       |   "previousname_9_condate":"",
       |   "previousname_9_companyname":"",
       |   "previousname_10_condate":"",
       |   "previousname_10_companyname":"",
       |   "confstmtnextduedate":"24/01/2018",
       |   "confstmtlastmadeupdate":"10/01/2017",
       |   "ref_period":"201706"
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
  info("I want to retrieve Companies House unit details for a period in time")
  info("So that I can view Companies House unit details via the user interface")

  feature("retrieve existing Companies House unit details") {
    scenario("by company reference number for a specific period") { wsClient =>
      Given(s"a unit link exists for a unit with $TargetCompanyRefNumber for $TargetPeriod")
      stubSbrControlApiFor(aCompaniesHouseUnitLinksRequest(withCompanyRefNumber = TargetCompanyRefNumber, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(CompaniesHouseUnitLinksResponseBody)
      ))
      And(s"Companies House admin data exists with $TargetCompanyRefNumber for $TargetPeriod")
      stubAdminDataApiFor(aCompaniesHouseForPeriodRequest(withCompanyRefNumber = TargetCompanyRefNumber, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(CompaniesHouseUnitResponseBody)
      ))

      When(s"the Companies House unit data with $TargetCompanyRefNumber is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/crns/${TargetCompanyRefNumber.value}").get())

      Then(s"the details of the Companies House unit with $TargetCompanyRefNumber for $TargetPeriod are returned")
      response.status shouldBe OK
      response.header(CONTENT_TYPE).value shouldBe JSON
      response.json shouldBe Json.parse(ExpectedDetailsBody)
    }
  }

  feature("retrieve non-existent Companies House unit details") {
    scenario("when there is no Companies House admin data with the specified company reference number and period") { wsClient =>
      Given(s"a unit link exists for a unit with $TargetCompanyRefNumber for $TargetPeriod")
      stubSbrControlApiFor(aCompaniesHouseUnitLinksRequest(withCompanyRefNumber = TargetCompanyRefNumber, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(CompaniesHouseUnitLinksResponseBody)
      ))
      And(s"Companies House admin data does not exist with $TargetCompanyRefNumber for $TargetPeriod")
      stubAdminDataApiFor(aCompaniesHouseForPeriodRequest(withCompanyRefNumber = TargetCompanyRefNumber, withPeriod = TargetPeriod).willReturn(
        aNotFoundResponse()
      ))

      When(s"the Companies House unit data with $TargetCompanyRefNumber is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/crns/${TargetCompanyRefNumber.value}").get())

      Then("a NOT FOUND response is returned")
      response.status shouldBe NOT_FOUND
    }

    scenario("when there are no unit links defined for the specified Companies House company reference number and period") { wsClient =>
      Given(s"a unit link does not exist for a unit with $TargetCompanyRefNumber for $TargetPeriod")
      stubSbrControlApiFor(aCompaniesHouseUnitLinksRequest(withCompanyRefNumber = TargetCompanyRefNumber, withPeriod = TargetPeriod).willReturn(
        aNotFoundResponse()
      ))

      When(s"the Companies House unit data with $TargetCompanyRefNumber is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/crns/${TargetCompanyRefNumber.value}").get())

      Then("a NOT FOUND response is returned")
      response.status shouldBe NOT_FOUND
    }
  }

  feature("handle failure gracefully") {
    scenario("when the retrieval of Companies House admin data returns an error") { wsClient =>
      Given(s"a unit link exists for a unit with $TargetCompanyRefNumber for $TargetPeriod")
      stubSbrControlApiFor(aCompaniesHouseUnitLinksRequest(withCompanyRefNumber = TargetCompanyRefNumber, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(CompaniesHouseUnitLinksResponseBody)
      ))
      And(s"retrieval of Companies House admin data with $TargetCompanyRefNumber for $TargetPeriod will return an error")
      stubAdminDataApiFor(aCompaniesHouseForPeriodRequest(withCompanyRefNumber = TargetCompanyRefNumber, withPeriod = TargetPeriod).willReturn(
        anInternalServerError()
      ))

      When(s"the Companies House unit data with $TargetCompanyRefNumber is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/crns/${TargetCompanyRefNumber.value}").get())

      Then("an INTERNAL_SERVER_ERROR response is returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }

    scenario("when the retrieval of Companies House unit links returns an error") { wsClient =>
      Given(s"retrieval of a unit with $TargetCompanyRefNumber for $TargetPeriod will return an error")
      stubSbrControlApiFor(aCompaniesHouseUnitLinksRequest(withCompanyRefNumber = TargetCompanyRefNumber, withPeriod = TargetPeriod).willReturn(
        anInternalServerError()
      ))

      When(s"the Companies House unit data with $TargetCompanyRefNumber is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/crns/${TargetCompanyRefNumber.value}").get())

      Then("an INTERNAL_SERVER_ERROR response is returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }

    scenario("the Companies House admin data service is unavailable") { wsClient =>
      Given(s"a unit link exists for a unit with $TargetCompanyRefNumber for $TargetPeriod")
      stubSbrControlApiFor(aCompaniesHouseUnitLinksRequest(withCompanyRefNumber = TargetCompanyRefNumber, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(CompaniesHouseUnitLinksResponseBody)
      ))
      And("the Companies House admin data service is unavailable")
      stopMockAdminDataApi()

      When(s"the Companies House unit data with $TargetCompanyRefNumber is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/crns/${TargetCompanyRefNumber.value}").get())

      Then("a server error is returned")
      response.status shouldBe aServerError
    }

    scenario("the sbr_control_api service is unavailable") { wsClient =>
      Given("the sbr_control_api service is unavailable")
      stopMockSbrControlApi()

      When(s"the Companies House unit data with $TargetCompanyRefNumber is requested for $TargetPeriod")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/crns/${TargetCompanyRefNumber.value}").get())

      Then("a server error is returned")
      response.status shouldBe aServerError
    }
  }

  feature("validate request parameters") {
    scenario("rejecting a Companies House company number that is too long") { wsClient =>
      Given("that a Companies House company reference number is represented by an eight digit number")

      When("the user requests a Companies House unit with a reference more than eight digits long")
      val response = await(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/crns/123456789").get())

      Then("a BAD REQUEST response is returned")
      response.status shouldBe BAD_REQUEST
    }
  }
}

