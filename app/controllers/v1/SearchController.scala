package controllers.v1

import javax.inject.Inject

import io.swagger.annotations._
import play.api.mvc.{ Action, AnyContent, Result }
import utils.Utilities.errAsJson
import utils.FutureResponse._

import scala.util.Try
import config.Properties.{ controlEndpoint, minKeyLength }
import play.api.libs.json.JsValue
import play.api.libs.ws.WSResponse
import uk.gov.ons.sbr.models.{ Enterprise, LegalUnit }
import utils.CsvProcessor.enterpriseFile
import services.WSRequest.RequestGenerator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }
import config.Properties.businessIndexRoute

import scala.concurrent.duration.Duration

@Api("Search")
class SearchController @Inject() (ws: RequestGenerator) extends ControllerUtils {

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
  def searchById(
    @ApiParam(value = "An identifier of any type", example = "825039145000", required = true) id: Option[String],
    @ApiParam(value = "term to categories the id source", required = false) origin: Option[String] = None
  ): Action[AnyContent] = {
    Action.async { implicit request =>
      val key = id.orElse(request.getQueryString("id"))
      //      val date = period.orElse(request.getQueryString("period"))
      val res: Future[Result] = key match {
        case Some(k) if k.length >= minKeyLength =>
          ws.singleRequest(k) map { response =>
            if (response.status == 200) {
              val unitMap = response.json.as[Seq[JsValue]].map(x =>
                (x \ "unitType").as[String] -> (x \ "id").as[String]).toMap
              val j = ws.multiRequest(unitMap)
              Ok(s" ${response.body},$j").as(JSON)
            } else NotFound(response.body).as(JSON)
          } recover responseException
        case _ => BadRequest(errAsJson(BAD_REQUEST, "invalid_key_size", s"missing key or key is too short [$minKeyLength]")).future
      }
      res
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
  def searchByLeU(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async { request =>
    logger.info(s"Sending request to Business Index for legal unit: $id")
    val res = id match {
      case id if id.length >= minKeyLength =>
        logger.info(s"Sending request to Business Index for legal unit id: $id")
        val resp = ws.singleRequestNoTimeout(s"$businessIndexRoute$id") map { response =>
          if (response.status == 200) {
            Ok(response.body).as(JSON)
          } else NotFound(response.body).as(JSON)
        } recover responseException
        resp
      case _ => BadRequest(errAsJson(BAD_REQUEST, "missing_parameter", "No query string found")).future
    }
    res
  }

  /**
   * @note - key or id
   */
  def searchByEnterprise(
    @ApiParam(value = "An identifier of any type", example = "825039145000", required = true) id: Long
  ): Action[AnyContent] = {
    Action.async { request =>
      val key = getQueryString(request, "id")
      retrieveRecord[Enterprise](key, enterpriseFile, Enterprise.fromMap, Enterprise.toJson)
    }
  }

  def searchByVat(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: Long
  ): Action[AnyContent] = Action.async { request =>
    val key: String = getQueryString(request, "id")
    Ok("").future
  }

  def searchByPaye(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async { request =>
    val key: String = getQueryString(request, "id")
    Ok("").future
  }

  def searchByCrn(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async { request =>
    val key: String = getQueryString(request, "id")
    Ok("").future
  }

}
