package controllers.v1

import javax.inject.Inject

import com.netaporter.uri.Uri
import io.swagger.annotations.{ Api, ApiOperation, ApiParam, ApiResponse, ApiResponses }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.mvc.{ Action, AnyContent, Result }
import play.api.libs.json.{ JsValue, Reads }
import utils.UriBuilder.uriPathBuilder
import utils.Utilities.errAsJson
import utils.FutureResponse.futureSuccess
import uk.gov.ons.sbr.models.UnitTypesShortNames._
import services.WSRequest.RequestGenerator
import config.Properties.{ minKeyLength, sbrControlApiBase }

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
      val key = id.orElse(request.getQueryString("id")).getOrElse("")
      search[UnitLinksListType](key, uriPathBuilder(sbrControlApiBase, key), individualSearch = false)
    }
  }

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
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) period: Option[String],
    @ApiParam(value = "An identifier of any type", example = "825039145000", required = true) id: Option[String]
  ): Action[AnyContent] = {
    Action.async { implicit request =>
      val key = id.orElse(request.getQueryString("id")).getOrElse("")
      val date = period.orElse(request.getQueryString("period")).getOrElse("")
      val res = date match {
        case x if x.length == fixedYeaMonthSize =>
          // todo - apply FUNC: uriPathBuilder on all uri creations
          search[UnitLinksListType](key, uriPathBuilder(sbrControlApiBase, key, Some(date)), periodParam = Some(date), individualSearch = false)
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
    search[StatisticalUnitLinkType](id, uriPathBuilder(sbrControlApiBase, id, types = Some(LEGAL_UNIT_TYPE)), LEGAL_UNIT_TYPE)
  }

  def searchEnterprise(
    @ApiParam(value = "An identifier of any type", example = "825039145000", required = true) id: String
  ): Action[AnyContent] = {
    Action.async { implicit request =>
      logger.info(s"Sending request to Control Api to retrieve enterprise with $id")
      search[StatisticalUnitLinkType](id, uriPathBuilder(sbrControlApiBase, id, types = Some(ENTERPRISE_TYPE)), ENTERPRISE_TYPE)
    }
  }

  def searchVat(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve VAT reference with $id")
    search[StatisticalUnitLinkType](id, uriPathBuilder(sbrControlApiBase, id, types = Some(VAT_REFERENCE_TYPE)), VAT_REFERENCE_TYPE)
  }

  def searchPaye(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve PAYE record with $id")
    search[StatisticalUnitLinkType](id, uriPathBuilder(sbrControlApiBase, id, types = Some(PAYE_TYPE)), PAYE_TYPE)
  }

  def searchCrn(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve Companies House Number with $id")
    search[StatisticalUnitLinkType](id, uriPathBuilder(sbrControlApiBase, id, types = Some(COMPANIES_HOUSE_REFERENCE_NUMBER_TYPE)), COMPANIES_HOUSE_REFERENCE_NUMBER_TYPE)
  }

  // equiv. with period routes
  def searchLeUWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) date: String,
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Control Api to retrieve enterprise with $id and $date")
    NotImplemented("Route not implemented. Please use searchLeU [without period param] -> route /v1/leus/:id").future
  }

  def searchEnterpriseWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) date: String,
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Control Api to retrieve enterprise with $id and $date")
    search[StatisticalUnitLinkType](id, uriPathBuilder(sbrControlApiBase, id, Some(date), Some(ENTERPRISE_TYPE)), ENTERPRISE_TYPE, Some(date))
  }

  def searchVatWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) date: String,
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve VAT reference with $id and $date")
    search[StatisticalUnitLinkType](id, uriPathBuilder(sbrControlApiBase, id, Some(date), Some(VAT_REFERENCE_TYPE)), VAT_REFERENCE_TYPE, Some(date))
  }

  def searchPayeWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) date: String,
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve PAYE record with $id and $date")
    search[StatisticalUnitLinkType](id, uriPathBuilder(sbrControlApiBase, id, Some(date), Some(PAYE_TYPE)), PAYE_TYPE, Some(date))
  }

  def searchCrnWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) date: String,
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve Companies House Number with $id and $date")
    search[StatisticalUnitLinkType](id, uriPathBuilder(sbrControlApiBase, id, Some(date), Some(COMPANIES_HOUSE_REFERENCE_NUMBER_TYPE)), COMPANIES_HOUSE_REFERENCE_NUMBER_TYPE, Some(date))
  }

  private def search[T](key: String, baseUrl: Uri, group: String = "", periodParam: Option[String] = None, individualSearch: Boolean = true)(implicit fjs: Reads[T]): Future[Result] = {
    val res: Future[Result] = key match {
      case k if k.length >= minKeyLength =>
        ws.singleRequest(baseUrl) map {
          case response if response.status == OK => {
            val unitResp = response.json.as[T]
            unitResp match {
              // a list of UnitLinks
              case u: UnitLinksListType =>
                if (u.length == cappedDisplayNumber) {
                  val mapOfRecordKeys = Map((u.head \ "unitType").as[String] -> (u.head \ "id").as[String])
                  mergeJson(mapOfRecordKeys, periodParam, u, individualSearch)
                } else {
                  PartialContent(unitResp.toString).as(JSON)
                }
              // a single StatisticalUnitLink obj [arg: UnitType param]
              case u: StatisticalUnitLinkType =>
                val mapOfRecordKeys = Map(group -> (u \ "id").as[String])
                mergeJson(mapOfRecordKeys, periodParam, Seq(u), individualSearch)
            }
          }
          case response if response.status == NOT_FOUND => NotFound(response.body).as(JSON)
          //@todo - fix err control _
          //          case _ => BadRequest(errAsJson(BAD_REQUEST, "bad_request", "unknown error"))
        } recover responseException
      case _ =>
        BadRequest(errAsJson(BAD_REQUEST, "missing_param", s"missing key or key [$key] is too short [$minKeyLength]")).future
    }
    res
  }

  private def mergeJson(mapOfRecordKeys: Map[String, String], periodParam: Option[String],
    unitResp: Seq[JsValue], individualSearch: Boolean): Result = {
    val respRecords: List[JsValue] = ws.parsedRequest(mapOfRecordKeys, periodParam)
    val json = toJson(respRecords, unitResp, individualSearch)
    Ok(json).as(JSON)
  }

}
