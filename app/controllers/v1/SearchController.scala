package controllers.v1

import io.swagger.annotations._
import javax.inject.{ Inject, Singleton }
import play.api.Configuration
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.mvc.{ Action, AnyContent }
import services.RequestGenerator
import uk.gov.ons.sbr.models._
import utils.FutureResponse.futureSuccess
import utils.UriBuilder._

import scala.util.Try

@Api("Search")
@Singleton
class SearchController @Inject() (implicit ws: RequestGenerator, val configuration: Configuration,
    val messagesApi: MessagesApi) extends ControllerUtils with I18nSupport {

  // TODO - updated ApiResponses annotation
  //public api
  @ApiOperation(
    value = "Json id match or a list of unit conflicts",
    notes = "The matches can occur from any id field and multiple records can be matched",
    responseContainer = "JSONObject",
    code = 200,
    httpMethod = "GET"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 200, responseContainer = "JSONObject", message = "Success -> Record(s) found for id."),
    new ApiResponse(code = 400, responseContainer = "JSONObject", message = "Client Side Error -> Required " +
      "parameter was not found."),
    new ApiResponse(code = 404, responseContainer = "JSONObject", message = "Client Side Error -> Id not found."),
    new ApiResponse(code = 500, responseContainer = "JSONObject", message = "Server Side Error -> Request " +
      "could not be completed.")
  ))
  def searchById(
    @ApiParam(value = "An identifier of any type", example = "825039145000", required = true) id: Option[String],
    @ApiParam(value = "A numerical limit", example = "6", required = false) history: Option[Int]
  ): Action[AnyContent] = {
    Action.async { implicit request =>
      val key = id.orElse(request.getQueryString("id")).getOrElse("")
      val limit = history.orElse(Try(Some(request.getQueryString("history").get.toInt)).getOrElse(None))
      val uri = createUri(SBR_CONTROL_API_URL, key)
      search[UnitLinksListType](key, uri, history = limit)
    }
  }

  // TODO - updated ApiResponses annotation
  //public api
  @ApiOperation(
    value = "Json id and period match or a list of unit conflicts",
    notes = "The matches can occur from any id field and multiple records can be matched",
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
  def searchByReferencePeriod(
    @ApiParam(value = "An identifier of any type", example = "825039145000", required = true) id: Option[String],
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) period: String
  ): Action[AnyContent] = {
    Action.async { implicit request =>
      val key = id.orElse(request.getQueryString("id")).getOrElse("")
      val res = period match {
        case x if x.length == FIXED_YEARMONTH_SIZE =>
          val uri = createUri(SBR_CONTROL_API_URL, key, Some(period))
          search[UnitLinksListType](key, uri, periodParam = Some(period))
        case _ => BadRequest(Messages("controller.invalid.period", period, "yyyyMM")).future
      }
      res
    }
  }

  // equiv. with period routes
  def searchLeUWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) date: String,
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    LOGGER.info(s"Sending request to Control Api to retrieve legal unit with $id and $date")
    val uri = createUri(SBR_CONTROL_API_URL, id, Some(date), Some(LEU))
    search[StatisticalUnitLinkType](id, uri, LEU, Some(date))
  }
}
