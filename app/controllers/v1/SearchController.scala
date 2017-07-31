package controllers.v1

import javax.inject.Inject

import io.swagger.annotations._
import play.api.mvc.{ Action, AnyContent }
import utils.Utilities.errAsJson
import com.outworkers.util.play._

import scala.util.Try
import models.units.{ Enterprise }
import utils.Properties._
import play.api.libs.ws.WSClient
import utils.CsvProcessor.{ sampleFile, enterpriseFile }
/**
 * Created by haqa on 04/07/2017.
 */
@Api("Search")
class SearchController @Inject() (ws: WSClient) extends ControllerUtils {

  //public api
  @ApiOperation(
    value = "Json list of id matches",
    notes = "The matches can occur from any id field and multiple records can be matched",
    responseContainer = "JSONObject",
    code = 200,
    httpMethod = "GET"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 200, response = classOf[Enterprise], responseContainer = "JSONObject", message = "Success -> Record(s) found for id."),
    new ApiResponse(code = 400, responseContainer = "JSONObject", message = "Client Side Error -> Required parameter was not found."),
    new ApiResponse(code = 404, responseContainer = "JSONObject", message = "Client Side Error -> Id not found."),
    new ApiResponse(code = 500, responseContainer = "JSONObject", message = "Server Side Error -> Request could not be completed.")
  ))
  def searchByEnterprise(
    @ApiParam(value = "An identifier of any type", example = "825039145000", required = true) id: Option[String],
    @ApiParam(value = "term to categories the id source", required = false) origin: Option[String]
  ): Action[AnyContent] = {
    Action.async { implicit request =>
      val key = Try(id.getOrElse(getQueryString(request).head)).getOrElse("")
      retrieveRecord[Enterprise](key, enterpriseFile, Enterprise.fromMap, Enterprise.toJson)
    }
  }

  //public api
  @ApiOperation(
    value = "Json Object of matching legal unit",
    notes = "Sends request to Business Index for legal units",
    responseContainer = "JSONObject",
    code = 200,
    httpMethod = "GET"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Success - Displays json list of dates for official development."),
    new ApiResponse(code = 500, responseContainer = "Json", message = "Internal Server Error - Request timed-out."),
    new ApiResponse(code = 500, responseContainer = "Json", message = "Internal Server Error - Failed to connection or timeout with endpoint.")
  ))
  def searchByUBRN(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: Long
  ): Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"Sending request to Business Index for legal unit: $id")
    val req: String = Try(getQueryString(request).head).getOrElse("")
    val res = req match {
      case id if id.length >= minKeyLength =>
        logger.info(s"Sending request to Business Index for legal unit id: $id")
        sendRequest(ws, s"$host:$id")
      case _ => BadRequest(errAsJson(BAD_REQUEST, "missing parameter", "No query string found")).future
    }
    res
  }

  def searchByVat(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: Long
  ): Action[AnyContent] = Action.async { implicit request =>
    val key: String = getQueryString(request).head
    retrieveRecord(key, sampleFile)
  }

  def searchByPaye(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async { implicit request =>
    val key: String = getQueryString(request).head
    retrieveRecord(key, sampleFile)
  }

  def searchByCrn(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async { implicit request =>
    val key: String = getQueryString(request).head
    retrieveRecord(key, sampleFile)
  }

}
