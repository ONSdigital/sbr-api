package controllers.v1

import javax.inject.Inject

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.netaporter.uri.Uri
import io.swagger.annotations._
import play.api.libs.json.{ JsValue, Json, Reads }
import play.api.mvc.{ Action, AnyContent, Result }

import uk.gov.ons.sbr.models._

import config.Properties.{ minKeyLength, sbrControlApiBase }
import utils.FutureResponse.futureSuccess
import utils.UriBuilder.uriPathBuilder
import utils.Utilities.errAsJson
import services.WSRequest.RequestGenerator

@Api("Search")
class SearchController @Inject() (ws: RequestGenerator) extends ControllerUtils {

  private type UnitLinksListType = Seq[JsValue]
  private type StatisticalUnitLinkType = JsValue

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
    @ApiParam(value = "An identifier of any type", example = "825039145000", required = true) id: Option[String]
  ): Action[AnyContent] = {
    Action.async { implicit request =>
      val key = id.orElse(request.getQueryString("id")).getOrElse("")
      search[UnitLinksListType](key, uriPathBuilder(sbrControlApiBase, key))
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
    @ApiParam(value = "An identifier of any type", example = "825039145000", required = true) id: Option[String],
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) period: String
  ): Action[AnyContent] = {
    Action.async { implicit request =>
      val key = id.orElse(request.getQueryString("id")).getOrElse("")
      val res = period match {
        case x if x.length == fixedYeaMonthSize =>
          search[UnitLinksListType](key, uriPathBuilder(sbrControlApiBase, key, Some(period)), periodParam = Some(period))
        case _ => BadRequest(errAsJson(BAD_REQUEST, "bad_request", s"Invalid date, try checking the length of given date $period")).future
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
    search[StatisticalUnitLinkType](id, uriPathBuilder(sbrControlApiBase, id, types = Some(LEU)), LEU)
  }

  def searchEnterprise(
    @ApiParam(value = "An identifier of any type", example = "825039145000", required = true) id: String
  ): Action[AnyContent] = {
    Action.async { implicit request =>
      logger.info(s"Sending request to Control Api to retrieve enterprise with $id")
      search[StatisticalUnitLinkType](id, uriPathBuilder(sbrControlApiBase, id, types = Some(ENT)), ENT)
    }
  }

  def searchVat(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve VAT reference with $id")
    search[StatisticalUnitLinkType](id, uriPathBuilder(sbrControlApiBase, id, types = Some(VAT)), VAT)
  }

  def searchPaye(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve PAYE record with $id")
    search[StatisticalUnitLinkType](id, uriPathBuilder(sbrControlApiBase, id, types = Some(PAYE)), PAYE)
  }

  def searchCrn(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve Companies House Number with $id")
    search[StatisticalUnitLinkType](id, uriPathBuilder(sbrControlApiBase, id, types = Some(CRN)), CRN)
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
    search[StatisticalUnitLinkType](id, uriPathBuilder(sbrControlApiBase, id, Some(date), Some(ENT)), ENT, Some(date))
  }

  def searchVatWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) date: String,
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve VAT reference with $id and $date")
    search[StatisticalUnitLinkType](id, uriPathBuilder(sbrControlApiBase, id, Some(date), Some(VAT)), VAT, Some(date))
  }

  def searchPayeWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) date: String,
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve PAYE record with $id and $date")
    search[StatisticalUnitLinkType](id, uriPathBuilder(sbrControlApiBase, id, Some(date), Some(PAYE)), PAYE, Some(date))
  }

  def searchCrnWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) date: String,
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve Companies House Number with $id and $date")
    search[StatisticalUnitLinkType](id, uriPathBuilder(sbrControlApiBase, id, Some(date), Some(CRN)), CRN, Some(date))
  }

  private def search[T](key: String, baseUrl: Uri, sourceType: DataSourceTypes = ENT, periodParam: Option[String]
    = None)(implicit fjs: Reads[T]): Future[Result] = {
    val res: Future[Result] = key match {
      case k if k.length >= minKeyLength =>
        ws.singleRequest(baseUrl) map {
          case response if response.status == OK => {
            val unitResp = response.json.as[T]
            unitResp match {
              case u: UnitLinksListType =>
                // if one UnitLinks found -> get unit
                if (u.length == cappedDisplayNumber) {
                  val mapOfRecordKeys = Map((u.head \ "unitType").as[String] -> (u.head \ "id").as[String])
                  val respRecords: List[JsValue] = ws.parsedRequest(mapOfRecordKeys, periodParam)
                  val json: Seq[JsValue] = (u zip respRecords).map(x => toJson(x))
                  Ok(Json.toJson(json)).as(JSON)
                } else {
                  // return UnitLinks if multiple
                  PartialContent(unitResp.toString).as(JSON)
                }
              case s: StatisticalUnitLinkType =>
                val mapOfRecordKeys = Map(sourceType.toString -> (s \ "id").as[String])
                val respRecords = ws.parsedRequest(mapOfRecordKeys, periodParam)
                val json = (Seq(s) zip respRecords).map(x => toJson(x)).head
                Ok(json).as(JSON)
            }
          }
          case response if response.status == NOT_FOUND => NotFound(response.body).as(JSON)
        } recover responseException
      case _ =>
        BadRequest(errAsJson(BAD_REQUEST, "missing_param", s"missing key or key [$key] is too short [$minKeyLength]")).future
    }
    res
  }

}
