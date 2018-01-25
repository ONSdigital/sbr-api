package controllers.v1

import javax.inject.Inject

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import play.api.libs.json.{ JsValue, Json, Reads }
import play.api.mvc.{ Action, AnyContent, Result }
import com.netaporter.uri.Uri
import io.swagger.annotations._

import uk.gov.ons.sbr.models._

import config.Properties.{ biBase, minKeyLength, sbrAdminBase, sbrControlApiBase }
import utils.FutureResponse.futureSuccess
import utils.UriBuilder.uriPathBuilder
import utils.Utilities.errAsJson

import services.RequestGenerator

/**
 * SearchController
 * ----------------
 * Author: haqa
 * Date: 10 July 2017 - 09:25
 * Copyright (c) 2017  Office for National Statistics
 */
@Api("Search")
class SearchController @Inject() (ws: RequestGenerator[Uri]) extends ControllerUtils {

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
      val uri = uriPathBuilder(sbrControlApiBase, key)
      search[UnitLinksListType](key, uri)
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
          val uri = uriPathBuilder(sbrControlApiBase, key, Some(period))
          search[UnitLinksListType](key, uri, periodParam = Some(period))
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
    val uri = uriPathBuilder(sbrControlApiBase, id, types = Some(LEU))
    search[StatisticalUnitLinkType](id, uri, LEU)
  }

  def searchEnterprise(
    @ApiParam(value = "An identifier of any type", example = "825039145000", required = true) id: String
  ): Action[AnyContent] = {
    Action.async { implicit request =>
      logger.info(s"Sending request to Control Api to retrieve enterprise with $id")
      val uri = uriPathBuilder(sbrControlApiBase, id, types = Some(ENT))
      search[StatisticalUnitLinkType](id, uri, ENT)
    }
  }

  def searchVat(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve VAT reference with $id")
    val uri = uriPathBuilder(sbrControlApiBase, id, types = Some(VAT))
    search[StatisticalUnitLinkType](id, uri, VAT)
  }

  def searchPaye(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve PAYE record with $id")
    val uri = uriPathBuilder(sbrControlApiBase, id, types = Some(PAYE))
    search[StatisticalUnitLinkType](id, uri, PAYE)
  }

  def searchCrn(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve Companies House Number with $id")
    val uri = uriPathBuilder(sbrControlApiBase, id, types = Some(CRN))
    search[StatisticalUnitLinkType](id, uri, CRN)
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
    val uri = uriPathBuilder(sbrControlApiBase, id, Some(date), Some(ENT))
    search[StatisticalUnitLinkType](id, uri, ENT, Some(date))
  }

  def searchVatWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) date: String,
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve VAT reference with $id and $date")
    val uri = uriPathBuilder(sbrControlApiBase, id, Some(date), Some(VAT))
    search[StatisticalUnitLinkType](id, uri, VAT, Some(date))
  }

  def searchPayeWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) date: String,
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve PAYE record with $id and $date")
    val uri = uriPathBuilder(sbrControlApiBase, id, Some(date), Some(PAYE))
    search[StatisticalUnitLinkType](id, uri, PAYE, Some(date))
  }

  def searchCrnWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) date: String,
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    logger.info(s"Sending request to Admin Api to retrieve Companies House Number with $id and $date")
    val uri = uriPathBuilder(sbrControlApiBase, id, Some(date), Some(CRN))
    search[StatisticalUnitLinkType](id, uri, CRN, Some(date))
  }

  // @ TODO - CHECK error control
  private def search[T](key: String, baseUrl: Uri, sourceType: DataSourceTypes = ENT,
    periodParam: Option[String] = None)(implicit fjs: Reads[T]): Future[Result] = {
    val res: Future[Result] = key match {
      case k if k.length >= minKeyLength =>
        ws.singleGETRequest(baseUrl) map {
          case response if response.status == OK => {
            val unitResp = response.json.as[T]
            unitResp match {
              case u: UnitLinksListType =>
                // if one UnitLinks found -> get unit
                if (u.length == cappedDisplayNumber) {
                  val mapOfRecordKeys = Map((u.head \ "unitType").as[String] -> (u.head \ "id").as[String])
                  val respRecords: List[JsValue] = parsedRequest(mapOfRecordKeys, periodParam)
                  val json: Seq[JsValue] = (u zip respRecords).map(toJson)
                  Ok(Json.toJson(json)).as(JSON)
                } else {
                  // return UnitLinks if multiple
                  PartialContent(unitResp.toString).as(JSON)
                }
              case s: StatisticalUnitLinkType =>
                val mapOfRecordKeys = Map(sourceType.toString -> (s \ "id").as[String])
                val respRecords = parsedRequest(mapOfRecordKeys, periodParam)
                val json = (Seq(s) zip respRecords).map(toJson).head
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

  // @TODO - duration.inf -> place cap
  def parsedRequest(searchList: Map[String, String], withPeriod: Option[String] = None): List[JsValue] = {
    searchList.map {
      case (group, id) =>
        // fix ch -> crn
        val filter = group match {
          case x if x == "CH" => "CRN"
          case x => x
        }
        val path = DataSourceTypesUtil.fromString(filter.toUpperCase) match {
          case Some(LEU) => biBase
          case Some(CRN | PAYE | VAT) => sbrAdminBase
          case Some(ENT) => sbrControlApiBase
        }
        val newPath = uriPathBuilder(path, id, withPeriod, group = filter)
        logger.info(s"Sending request to $newPath")
        val resp = ws.singleGETRequestWithTimeout(newPath, Duration.Inf)
        resp.json
    }.toList
  }

}
