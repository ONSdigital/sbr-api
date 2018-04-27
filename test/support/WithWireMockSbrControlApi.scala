package support

import com.github.tomakehurst.wiremock.client.WireMock.{ aResponse, get, urlEqualTo }
import com.github.tomakehurst.wiremock.client.{ MappingBuilder, ResponseDefinitionBuilder, WireMock }
import org.scalatest.Suite
import play.api.http.Status.{ INTERNAL_SERVER_ERROR, NOT_FOUND, OK }
import uk.gov.ons.sbr.models.{ Ern, Period, UnitId }

trait WithWireMockSbrControlApi extends WithWireMock { this: Suite =>
  override val wireMockPort = 9001

  def anEnterpriseUnitLinksRequest(withErn: Ern, withPeriod: Period): MappingBuilder =
    anEnterpriseUnitLinksRequest(UnitId(withErn.value), withPeriod)

  def anEnterpriseUnitLinksRequest(withId: UnitId, withPeriod: Period): MappingBuilder =
    get(urlEqualTo(s"/v1/periods/${Period.asString(withPeriod)}/types/ENT/units/${withId.value}"))

  def anEnterpriseForPeriodRequest(withErn: Ern, withPeriod: Period): MappingBuilder =
    get(urlEqualTo(s"/v1/periods/${Period.asString(withPeriod)}/enterprises/${withErn.value}"))

  def anOkResponse(): ResponseDefinitionBuilder =
    aResponse().withStatus(OK)

  def aNotFoundResponse(): ResponseDefinitionBuilder =
    aResponse().withStatus(NOT_FOUND)

  def anInternalServerError(): ResponseDefinitionBuilder =
    aResponse().withStatus(INTERNAL_SERVER_ERROR)

  val stubSbrControlApiFor: MappingBuilder => Unit =
    WireMock.stubFor
}
