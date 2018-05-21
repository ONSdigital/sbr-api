package support.wiremock

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import play.api.http.Status.{ INTERNAL_SERVER_ERROR, NOT_FOUND, OK }

trait ApiResponse {
  def anOkResponse(): ResponseDefinitionBuilder =
    aResponse().withStatus(OK)

  def aNotFoundResponse(): ResponseDefinitionBuilder =
    aResponse().withStatus(NOT_FOUND)

  def anInternalServerError(): ResponseDefinitionBuilder =
    aResponse().withStatus(INTERNAL_SERVER_ERROR)
}
