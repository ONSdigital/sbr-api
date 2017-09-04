package controllers.v1

import javax.inject.Inject

import io.swagger.annotations.{ Api, ApiOperation, ApiResponses, ApiResponse, ApiParam }
import play.api.mvc.{ Action, AnyContent, Result }
import utils.Utilities.errAsJson
import utils.FutureResponse.futureSuccess
import play.api.libs.json.JsValue
import services.WSRequest.RequestGenerator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import config.Properties._

@Api("Search")
class SearchController @Inject() (ws: RequestGenerator) extends ControllerUtils {

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
      search(key)
    }
  }

  /**
   * @note - rep. period param in url => remove replace
   */
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
    new ApiResponse(code = 400, responseContainer = "JSONObject", message = "Client Side Error -> Required parameter was not found."),
    new ApiResponse(code = 404, responseContainer = "JSONObject", message = "Client Side Error -> Id not found."),
    new ApiResponse(code = 500, responseContainer = "JSONObject", message = "Server Side Error -> Request could not be completed.")
  ))
  def searchByReferencePeriod(
    @ApiParam(value = "An identifier of any type", example = "825039145000", required = true) id: Option[String],
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) period: Option[String]
  ): Action[AnyContent] = {
    Action.async { implicit request =>
      val key = id.orElse(request.getQueryString("id"))
      val date = id.orElse(request.getQueryString("period")).getOrElse("")
      val res = date match {
        case x if x.length == fixedYeaMonthSize =>
          search(key, controlEndpointWithPeriod.replace(placeholderPeriod, date))
        case _ => BadRequest(errAsJson(BAD_REQUEST, "bad_request", s"Invalid date, try checking the length of given date $date")).future
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
  def searchLeU(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Business Index for legal unit: $id")
    unitSearch(id, businessIndexRoute)
  }

  def searchEnterprise(
    @ApiParam(value = "An identifier of any type", example = "825039145000", required = true) id: String
  ): Action[AnyContent] = {
    Action.async {
      logger.info(s"Sending request to Control Api to retrieve enterprise with $id")
      unitSearch(id, controlEnterpriseSearch)
    }
  }

  def searchVat(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve VAT reference with $id")
    unitSearch(id, adminVATsSearch)
  }

  def searchPaye(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve PAYE record with $id")
    unitSearch(id, adminPAYEsSearch)
  }

  def searchCrn(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve Companies House Number with $id")
    unitSearch(id, adminCompaniesSearch)
  }

  /**
   * @note - rep. period param in url => remove replace
   */
  def searchEnterpriseWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) date: String,
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Control Api to retrieve enterprise with $id and $date")
    unitSearch(id, enterpriseSearchWithPeriod.replace(placeholderPeriod, date))
  }

  def searchVatWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) date: String,
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve VAT reference with $id and $date")
    unitSearch(id, adminVATsSearchWithPeriod.replace(placeholderPeriod, date))
  }

  def searchPayeWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) date: String,
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve PAYE record with $id and $date")
    unitSearch(id, adminPAYEsSearchWithPeriod.replace(placeholderPeriod, date))
  }

  def searchCrnWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) date: String,
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve Companies House Number with $id and $date")
    unitSearch(id, adminCompaniesSearchWithPeriod.replace(placeholderPeriod, date))
  }

  /**
   * @note - trait ResponseMatch - for conflict or single result operations
   */
  private def search(key: Option[String], baseUrl: String = controlEndpoint): Future[Result] = {
    val res: Future[Result] = key match {
      case Some(k) if k.length >= minKeyLength =>
        ws.singleRequest(k, baseUrl) map {
          case response if response.status == OK => {
            val unitResp = response.json.as[Seq[JsValue]]
            if (unitResp.length == cappedDisplayNumber) {
              val mapOfRecordKeys = unitResp.map(x =>
                (x \ "unitType").as[String] -> (x \ "id").as[String]).toMap
              val respRecords: List[JsValue] = ws.multiRequest(mapOfRecordKeys)
              val json = toJson(respRecords, unitResp)
              Ok(json).as(JSON)
            } else
              PartialContent(unitResp.toString).as(JSON)
          }
          case response if response.status == NOT_FOUND => NotFound(response.body).as(JSON)
          //@todo - fix err control _
          case _ => BadRequest(errAsJson(BAD_REQUEST, "bad_request", "unknown error"))
        } recover responseException
      case _ =>
        BadRequest(errAsJson(BAD_REQUEST, "missing_param", s"missing key or key [$key] is too short [$minKeyLength]")).future
    }
    res
  }

  // @todo - check NotFound flow
  private def unitSearch(id: String, url: String): Future[Result] = {
    val res = id match {
      case id if id.length >= minKeyLength =>
        logger.info(s"Checking id length: $id")
        val resp = ws.singleRequestNoTimeout(s"$url$id") map { response =>
          if (response.status == OK) {
            Ok(response.body).as(JSON)
          } else NotFound(response.body).as(JSON)
        } recover responseException
        resp
      case _ => BadRequest(errAsJson(BAD_REQUEST, "missing_parameter", "No query string found")).future
    }
    res
  }

}
