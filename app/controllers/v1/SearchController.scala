package controllers.v1

import javax.inject.Inject

import io.swagger.annotations._
import play.api.mvc.{ Action, AnyContent, Result }
import utils.Utilities.errAsJson
import com.outworkers.util.play._
import play.api.Environment
import scala.util.Try
import models.units.{ Enterprise, EnterpriseObj }
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future, TimeoutException }
import scala.concurrent.duration._

/**
 * Created by haqa on 04/07/2017.
 */
@Api("Search")
class SearchController @Inject() (ws: WSClient) extends ControllerUtils {
  // (implicit config: Config)

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
    @ApiParam(value = "term to categories the id source", required = false) origin: Option[String]
  ): Action[AnyContent] = {
    Action.async { implicit request =>
      val key = Try(id.getOrElse(getQueryString(request).head.toString)).getOrElse("")
      val res = key match {
        case key if key.length > minLengthKey => findRecord(key, "/sample/enterprise.csv") match {
          case Nil =>
            logger.debug(s"No record found for id: ${id}")
            NotFound(errAsJson(404, "not found", s"Could not find value ${id}")).future
          case x => Ok(s"""${EnterpriseObj.toString(EnterpriseObj.toMap, x)}""").as(JSON).future
        }
        case _ => BadRequest(errAsJson(400, "missing parameter", "No query string found")).future
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
  def searchByUBRN(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async { implicit request =>
    logger.info(s"Sending request to Business Index for legal unit: ${id}")
    /**
     * @todo - move url and host val to app.conf
     */
    val req = Try(id.orElse(getQueryString(request).head.toString)).getOrElse("")
    println(s"HOSTTT: ${host}")
    val res = req match {
      case i => sendRequest(s"${host}:${i}")
      case _ => BadRequest(errAsJson(400, "missing parameter", "No query string found")).future
    }
    res
  }

  def sendRequest(url: String): Future[Result] = {
    val res = ws.url(url).withRequestTimeout(5000.millis).get().map {
      response =>
        Ok(response.body).as(JSON)
    } recover {
      case t: TimeoutException =>
        RequestTimeout(errAsJson(408, "request_timeout", "This may be due to connection being blocked."))
      case e =>
        ServiceUnavailable(errAsJson(503, "service_unavailable", "Cannot Connect to host. Please verify the address is correct."))
      case x =>
        BadRequest(errAsJson(404, "bad_request", "Cannot find specified id."))
    }
    res
  }

}