package controllers.v1

import javax.inject.{ Inject, Singleton }

import play.api.Configuration
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ Action, AnyContent }
import io.swagger.annotations._

import utils.UriBuilder._
import services.RequestGenerator

@Api("Local Unit")
@Singleton
class LocalUnitController @Inject() (implicit ws: RequestGenerator, val configuration: Configuration,
    val messagesApi: MessagesApi) extends ControllerUtils with I18nSupport {

  @ApiOperation(
    value = "The local unit record",
    notes = "An exact match will be returned",
    responseContainer = "JSONObject",
    code = 200,
    httpMethod = "GET"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 200, responseContainer = "JSONObject", message = "Success -> Record(s) found for id."),
    new ApiResponse(code = 400, responseContainer = "JSONObject", message = "Client Side Error -> Required " +
      "parameter was not found."),
    new ApiResponse(code = 404, responseContainer = "JSONObject", message = "Client Side Error -> Id not found."),
    new ApiResponse(code = 500, responseContainer = "JSONObject", message = "Server Side Error -> " +
      "Request could not be completed.")
  ))
  def searchLoUWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "201803", required = true) date: String,
    @ApiParam(value = "A local unit identifier", example = "900000123", required = true) id: String
  ): Action[AnyContent] = Action.async {
    LOGGER.info(s"Sending request to Control Api to retrieve local unit with $id and $date")
    val uri = createLouPeriodUri(SBR_CONTROL_API_URL, id, date)
    louSearch(uri, id, date)
  }
}