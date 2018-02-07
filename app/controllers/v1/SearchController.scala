package controllers.v1

import javax.inject.Inject

import play.api.Configuration
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.mvc.{ Action, AnyContent }
import io.swagger.annotations._

import uk.gov.ons.sbr.models._

import utils.FutureResponse.futureSuccess
import utils.UriBuilder.uriPathBuilder
import services.RequestGenerator

/**
 * SearchController
 * ----------------
 * Author: haqa
 * Date: 10 July 2017 - 09:25
 * Copyright (c) 2017  Office for National Statistics
 */
@Api("Search")
class SearchController @Inject() (implicit ws: RequestGenerator, val configuration: Configuration,
    val messagesApi: MessagesApi) extends ControllerUtils with I18nSupport {

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
    @ApiParam(value = "An identifier of any type", example = "825039145000", required = true) id: Option[String]
  ): Action[AnyContent] = {
    Action.async { implicit request =>
      val key = id.orElse(request.getQueryString("id")).getOrElse("")
      val uri = uriPathBuilder(sbrControlApiURL, key)
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
          val uri = uriPathBuilder(sbrControlApiURL, key, Some(period))
          search[UnitLinksListType](key, uri, periodParam = Some(period))
        case _ => BadRequest(Messages("controller.invalid.period", period, "yyyyMM")).future
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
    new ApiResponse(code = 500, responseContainer = "Json", message = "Internal Server Error - " +
      "Failed to connection or timeout with endpoint.")
  ))
  def searchLeU(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    LOGGER.info(s"Sending request to Business Index for legal unit: $id")
    val uri = uriPathBuilder(sbrControlApiURL, id, types = Some(LEU))
    search[StatisticalUnitLinkType](id, uri, LEU)
  }

  def searchEnterprise(
    @ApiParam(value = "An identifier of any type", example = "825039145000", required = true) id: String
  ): Action[AnyContent] = {
    Action.async { implicit request =>
      LOGGER.info(s"Sending request to Control Api to retrieve enterprise with $id")
      val uri = uriPathBuilder(sbrControlApiURL, id, types = Some(ENT))
      search[StatisticalUnitLinkType](id, uri, ENT)
    }
  }

  def searchVat(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    LOGGER.info(s"Sending request to Admin Api to retrieve VAT reference with $id")
    val uri = uriPathBuilder(sbrControlApiURL, id, types = Some(VAT))
    search[StatisticalUnitLinkType](id, uri, VAT)
  }

  def searchPaye(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    LOGGER.info(s"Sending request to Admin Api to retrieve PAYE record with $id")
    val uri = uriPathBuilder(sbrControlApiURL, id, types = Some(PAYE))
    search[StatisticalUnitLinkType](id, uri, PAYE)
  }

  def searchCrn(
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    LOGGER.info(s"Sending request to Admin Api to retrieve Companies House Number with $id")
    val uri = uriPathBuilder(sbrControlApiURL, id, types = Some(CRN))
    search[StatisticalUnitLinkType](id, uri, CRN)
  }

  // equiv. with period routes
  def searchLeUWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) date: String,
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    LOGGER.info(s"Sending request to Control Api to retrieve legal unit with $id and $date")
    val uri = uriPathBuilder(sbrControlApiURL, id, Some(date), Some(LEU))
    search[StatisticalUnitLinkType](id, uri, LEU, Some(date))
  }

  def searchEnterpriseWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) date: String,
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    LOGGER.info(s"Sending request to Control Api to retrieve enterprise with $id and $date")
    val uri = uriPathBuilder(sbrControlApiURL, id, Some(date), Some(ENT))
    search[StatisticalUnitLinkType](id, uri, ENT, Some(date))
  }

  def searchVatWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) date: String,
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    LOGGER.info(s"Sending request to Admin Api to retrieve VAT reference with $id and $date")
    val uri = uriPathBuilder(sbrControlApiURL, id, Some(date), Some(VAT))
    search[StatisticalUnitLinkType](id, uri, VAT, Some(date))
  }

  def searchPayeWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) date: String,
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    LOGGER.info(s"Sending request to Admin Api to retrieve PAYE record with $id and $date")
    val uri = uriPathBuilder(sbrControlApiURL, id, Some(date), Some(PAYE))
    search[StatisticalUnitLinkType](id, uri, PAYE, Some(date))
  }

  def searchCrnWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) date: String,
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async {
    LOGGER.info(s"Sending request to Admin Api to retrieve Companies House Number with $id and $date")
    val uri = uriPathBuilder(sbrControlApiURL, id, Some(date), Some(CRN))
    search[StatisticalUnitLinkType](id, uri, CRN, Some(date))
  }
}
