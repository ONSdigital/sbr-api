package support.wiremock

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.{ get, urlEqualTo }
import com.typesafe.scalalogging.LazyLogging
import uk.gov.ons.sbr.models.{ CompanyRefNumber, PayeRef, Period, VatRef }

trait WireMockAdminDataApi extends ApiResponse with LazyLogging {
  val DefaultAdminDataApiPort = 9005
  private var wireMockSupportContainer: Option[WireMockSupport] = None

  def startMockAdminDataApi(port: Int = DefaultAdminDataApiPort): Unit = {
    logger.debug(s"Starting WireMockAdminDataApi on port [$port].")
    wireMockSupportContainer = Some(WireMockSupport.start(port))
  }

  def stopMockAdminDataApi(): Unit = {
    wireMockSupportContainer.foreach { wm =>
      logger.debug(s"Stopping WireMockAdminDataApi on port [${WireMockSupport.port(wm)}].")
      WireMockSupport.stop(wm)
    }
    wireMockSupportContainer = None
  }

  def withWireMockAdminDataApi[A](fn: () => A): A = {
    startMockAdminDataApi()
    try fn()
    finally stopMockAdminDataApi()
  }

  def stubAdminDataApiFor(mappingBuilder: MappingBuilder): Unit = {
    require(wireMockSupportContainer.isDefined, "WireMockAdminDataApi must be started before it can be stubbed")
    wireMockSupportContainer.foreach(wm => WireMockSupport.registerMapping(wm)(mappingBuilder))
  }

  def aVatForPeriodRequest(withVatRef: VatRef, withPeriod: Period): MappingBuilder =
    get(urlEqualTo(s"/v1/records/${withVatRef.value}/periods/${Period.asString(withPeriod)}"))

  def aPayeForPeriodRequest(withPayeRef: PayeRef, withPeriod: Period): MappingBuilder =
    get(urlEqualTo(s"/v1/records/${withPayeRef.value}/periods/${Period.asString(withPeriod)}"))

  def aCompaniesHouseForPeriodRequest(withCompanyRefNumber: CompanyRefNumber, withPeriod: Period): MappingBuilder =
    get(urlEqualTo(s"/v1/records/${withCompanyRefNumber.value}/periods/${Period.asString(withPeriod)}"))
}
