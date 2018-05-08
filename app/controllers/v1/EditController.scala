package controllers.v1

import io.swagger.annotations._
import javax.inject.Inject
import play.api.i18n.MessagesApi
import play.api.libs.json._
import play.api.mvc.{ Action, AnyContent, Result }
import play.api.{ Configuration, Logger }
import services.RequestGenerator
import utils.FutureResponse.futureSuccess
import utils.Utilities._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Api("Edit")
class EditController @Inject() (ws: RequestGenerator, val configuration: Configuration, val messagesApi: MessagesApi) extends ControllerUtils {
  // TODO: Fix CORS issue to allow use of Content-Type: application/json
  // There is a CORS issue meaning the UI cannot do a POST request with the headers:
  // Content-Type: application/json
  // There is a temporary fix below, to just parse the POST body as text and do Json.parse(text).

  @ApiOperation(
    value = "Ok if edit is made",
    notes = "Invokes a method in sbr-hbase-connector to edit an Enterprise",
    responseContainer = "JSONObject",
    code = 200,
    httpMethod = "POST"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 200, responseContainer = "JsValue", message = "Edit has been made successfully to Enterprise with id: [id]"),
    new ApiResponse(code = 400, responseContainer = "JsValue", message = "BadRequest -> id or edit json is invalid"),
    new ApiResponse(code = 500, responseContainer = "JsValue", message = "InternalServerError -> Unable to make edit")
  ))
  def editEnterprise(
    @ApiParam(value = "An Enterprise ID", example = "1234567890", required = true) id: String
  ): Action[AnyContent] = Action.async { implicit request =>
    val url = s"$CONTROL_EDIT_ENTERPRISE_URL$id"
    val jsonBody: Option[String] = request.body.asText
    Logger.info(s"Rerouting edit enterprise by default period request to: $url")
    rerouteEditPost(jsonBody, url)
  }

  @ApiOperation(
    value = "Ok if edit is made",
    notes = "Invokes a method in sbr-hbase-connector to edit an Enterprise",
    responseContainer = "JSONObject",
    code = 200,
    httpMethod = "POST"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 200, responseContainer = "JsValue", message = "Edit has been made successfully to Enterprise with id: [id]"),
    new ApiResponse(code = 400, responseContainer = "JsValue", message = "BadRequest -> id or edit json or period is invalid"),
    new ApiResponse(code = 500, responseContainer = "JsValue", message = "InternalServerError -> Unable to make edit")
  ))
  def editEnterpriseForPeriod(
    @ApiParam(value = "A period in yyyyMM format", example = "201706", required = true) period: String,
    @ApiParam(value = "An Enterprise ID", example = "1234567890", required = true) id: String
  ): Action[AnyContent] = Action.async { implicit request =>
    val url = s"${CONTROL_EDIT_ENTERPRISE_URL.replace(PLACEHOLDER_PERIOD, period)}$id"
    val jsonBody: Option[String] = request.body.asText
    Logger.info(s"Rerouting edit enterprise by specified period request to: $url")
    rerouteEditPost(jsonBody, url)
  }

  def rerouteEditPost(jsonBody: Option[String], url: String): Future[Result] = {
    jsonBody.map { text =>
      ws.singlePOSTRequest(url, headers = CONTENT_TYPE -> JSON, body = Json.parse(text.toString)).map { response =>
        Status(response.status)(response.body)
      }
    }.getOrElse {
      Logger.debug(s"Invalid JSON for redirect to url: $url")
      BadRequest(errAsJson(msg = "POST body json is malformed")).future
    }
  }
}
