package support.wiremock

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.{ get, urlEqualTo }
import com.typesafe.scalalogging.LazyLogging
import uk.gov.ons.sbr.models.{ Ern, Period, UnitId, VatRef }

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

  def aVatUnitLinksRequest(withVatRef: VatRef, withPeriod: Period): MappingBuilder =
    get(urlEqualTo(s"/v1/periods/${Period.asString(withPeriod)}/types/VAT/units/${withVatRef.value}"))
}
