package support.wiremock

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.{ get, patch, urlEqualTo }
import com.typesafe.scalalogging.LazyLogging
import uk.gov.ons.sbr.models._

trait WireMockSbrControlApi extends ApiResponse with LazyLogging {
  val DefaultSbrControlApiPort = 9001
  private var wireMockSupportContainer: Option[WireMockSupport] = None

  def startMockSbrControlApi(port: Int = DefaultSbrControlApiPort): Unit = {
    logger.debug(s"Starting WireMockSbrControlApi on port [$port].")
    wireMockSupportContainer = Some(WireMockSupport.start(port))
  }

  def stopMockSbrControlApi(): Unit = {
    wireMockSupportContainer.foreach { wm =>
      logger.debug(s"Stopping WireMockSbrControlApi on port [${WireMockSupport.port(wm)}].")
      WireMockSupport.stop(wm)
    }
    wireMockSupportContainer = None
  }

  def withWireMockSbrControlApi[A](fn: () => A): A = {
    startMockSbrControlApi()
    try fn()
    finally stopMockSbrControlApi()
  }

  def stubSbrControlApiFor(mappingBuilder: MappingBuilder): Unit = {
    require(wireMockSupportContainer.isDefined, "WireMockSbrControlApi must be started before it can be stubbed")
    wireMockSupportContainer.foreach(wm => WireMockSupport.registerMapping(wm)(mappingBuilder))
  }

  def anEnterpriseUnitLinksRequest(withErn: Ern, withPeriod: Period): MappingBuilder =
    anEnterpriseUnitLinksRequest(UnitId(withErn.value), withPeriod)

  def anEnterpriseUnitLinksRequest(withId: UnitId, withPeriod: Period): MappingBuilder =
    get(urlEqualTo(s"/v1/periods/${Period.asString(withPeriod)}/types/ENT/units/${withId.value}"))

  def anEnterpriseForPeriodRequest(withErn: Ern, withPeriod: Period): MappingBuilder =
    get(urlEqualTo(s"/v1/periods/${Period.asString(withPeriod)}/enterprises/${withErn.value}"))

  def aVatParentLinkEditRequest(withVatRef: VatRef, withPeriod: Period): MappingBuilder =
    patch(urlEqualTo(s"/v1/periods/${Period.asString(withPeriod)}/types/VAT/units/${withVatRef.value}"))

  def aLegalUnitChildLinkCreationRequest(withUbrn: UnitId, withPeriod: Period): MappingBuilder =
    patch(urlEqualTo(s"/v1/periods/${Period.asString(withPeriod)}/types/LEU/units/${withUbrn.value}"))

  def aVatUnitLinksRequest(withVatRef: VatRef, withPeriod: Period): MappingBuilder =
    get(urlEqualTo(s"/v1/periods/${Period.asString(withPeriod)}/types/VAT/units/${withVatRef.value}"))

  def aPayeUnitLinksRequest(withPayeRef: PayeRef, withPeriod: Period): MappingBuilder =
    get(urlEqualTo(s"/v1/periods/${Period.asString(withPeriod)}/types/PAYE/units/${withPayeRef.value}"))

  def aCompaniesHouseUnitLinksRequest(withCompanyRefNumber: CompanyRefNumber, withPeriod: Period): MappingBuilder =
    get(urlEqualTo(s"/v1/periods/${Period.asString(withPeriod)}/types/CH/units/${withCompanyRefNumber.value}"))

  def aLocalUnitUnitLinksRequest(withLurn: Lurn, withPeriod: Period): MappingBuilder =
    get(urlEqualTo(s"/v1/periods/${Period.asString(withPeriod)}/types/LOU/units/${withLurn.value}"))

  def aLocalUnitForPeriodRequest(withErn: Ern, withLurn: Lurn, withPeriod: Period): MappingBuilder =
    get(urlEqualTo(s"/v1/enterprises/${withErn.value}/periods/${Period.asString(withPeriod)}/localunits/${withLurn.value}"))

  def aReportingUnitUnitLinksRequest(withRurn: Rurn, withPeriod: Period): MappingBuilder =
    get(urlEqualTo(s"/v1/periods/${Period.asString(withPeriod)}/types/REU/units/${withRurn.value}"))

  def aReportingUnitForPeriodRequest(withErn: Ern, withRurn: Rurn, withPeriod: Period): MappingBuilder =
    get(urlEqualTo(s"/v1/enterprises/${withErn.value}/periods/${Period.asString(withPeriod)}/reportingunits/${withRurn.value}"))
}
