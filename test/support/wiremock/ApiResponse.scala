package support.wiremock

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import play.api.http.Status.{ INTERNAL_SERVER_ERROR, NOT_FOUND, OK, NO_CONTENT, UNPROCESSABLE_ENTITY, CONFLICT }

trait ApiResponse {
  def anOkResponse(): ResponseDefinitionBuilder =
    aResponse().withStatus(OK)

  def aNoContentResponse(): ResponseDefinitionBuilder =
    aResponse().withStatus(NO_CONTENT)

  def aNotFoundResponse(): ResponseDefinitionBuilder =
    aResponse().withStatus(NOT_FOUND)

  def anUnprocessableEntityResponse(): ResponseDefinitionBuilder =
    aResponse().withStatus(UNPROCESSABLE_ENTITY)

  def aConflictResponse(): ResponseDefinitionBuilder =
    aResponse().withStatus(CONFLICT)

  def anInternalServerError(): ResponseDefinitionBuilder =
    aResponse().withStatus(INTERNAL_SERVER_ERROR)
}