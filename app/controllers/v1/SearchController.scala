package controllers.v1

import java.time.format.DateTimeParseException

import com.typesafe.scalalogging.StrictLogging
import config.Properties
import controllers.v1.SearchController.{CAPPED_DISPLAY_NUMBER, FIXED_YEARMONTH_SIZE}
import io.lemonlabs.uri.Uri
import io.swagger.annotations._
import javax.inject.{Inject, Singleton}
import javax.naming.ServiceUnavailableException
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration
import play.api.i18n.{Messages, MessagesProvider}
import play.api.libs.json.{JsValue, Json, _}
import play.api.libs.ws.WSResponse
import play.api.mvc._
import services.RequestGenerator
import uk.gov.ons.sbr.models._
import utils.FutureResponse.futureSuccess
import utils.UriBuilder.{createLouUri, createUri}
import utils.Utilities.{errAsJson, orElseNull}

import scala.concurrent.duration.Duration
import scala.concurrent.{Future, TimeoutException}
import scala.util.Try

@Api("Search")
@Singleton
@SuppressWarnings(Array("TraversableHead", "OptionGet"))    // disable scapegoat errors for this class
class SearchController @Inject() (val configuration: Configuration, mcc: MessagesControllerComponents)(implicit ws: RequestGenerator) extends MessagesAbstractController(mcc) with StrictLogging with Properties {

  private type UnitLinksListType = Seq[JsValue]
  private type StatisticalUnitLinkType = JsValue

  private[this] val LOGGER: Logger = LoggerFactory.getLogger(getClass.getName)
  private[this] implicit val executionContext = mcc.executionContext

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

  def searchLeUWithPeriod(
    @ApiParam(value = "Identifier creation date", example = "2017/07", required = true) date: String,
    @ApiParam(value = "A legal unit identifier", example = "<some example>", required = true) id: String
  ): Action[AnyContent] = Action.async { implicit request =>
    LOGGER.info(s"Sending request to Control Api to retrieve legal unit with $id and $date")
    val uri = createUri(SBR_CONTROL_API_URL, id, Some(date), Some(LEU))
    search[StatisticalUnitLinkType](id, uri, LEU, Some(date))
  }

  private def search[T](key: String, baseUrl: Uri, sourceType: DataSourceTypes = ENT, periodParam: Option[String] = None, history: Option[Int] = None)(implicit fjs: Reads[T], ws: RequestGenerator, messagesProvider: MessagesProvider): Future[Result] =
    key match {
      case k if k.length >= MINIMUM_KEY_LENGTH =>
        LOGGER.debug(s"Sending request to ${baseUrl.toString} to retrieve Unit Links")
        ws.singleGETRequest(baseUrl.toString) flatMap {
          case response if response.status == OK =>
            LOGGER.debug(s"Result for unit is: ${response.body}")
            // @ TODO - add to success or failure to JSON ??
            val unitResp = response.json.as[T]
            unitResp match {
              // UnitLinksListType is erased - the type of the content is unchecked
              case u: Seq[_] =>
                // if one UnitLinks found -> get unit
                if (u.length == CAPPED_DISPLAY_NUMBER) {
                  u.head match {
                    case j: JsValue =>
                      val id = (j \ "id").as[String]
                      LOGGER.debug(s"Found a single response with $id")
                      val unitType = (j \ "unitType").as[String]
                      val period = (j \ "period").as[String]
                      val mapOfRecordKeys = Map(unitType -> id)
                      parsedRequest(mapOfRecordKeys, j, periodParam, history).map { respRecords =>
                        val json = (Seq(j) zip respRecords).map(x => toJson(x, unitType, period))
                        Ok(Json.toJson(json)).as(JSON)
                      }
                    case _ =>
                      throw new AssertionError("Seq does not contain a JsValue")
                  }
                } else {
                  LOGGER.debug(s"Found multiple records matching given id, $key. Returning multiple as list.")
                  // return UnitLinks if multiple
                  PartialContent(unitResp.toString).as(JSON).future
                }
              case s: StatisticalUnitLinkType =>
                val mapOfRecordKeys = Map(sourceType.toString -> (s \ "id").as[String])
                val period = (s \ "period").as[String]
                parsedRequest(mapOfRecordKeys, s, periodParam).map { respRecords =>
                  val json = (Seq(s) zip respRecords).map(x => toJson(x, sourceType.toString, period)).head
                  Ok(json).as(JSON)
                }
            }
          case response if response.status == NOT_FOUND =>
            NotFound(response.body).as(JSON).future
        } recover responseException
      case _ =>
        BadRequest(Messages("controller.invalid.id", key, MINIMUM_KEY_LENGTH)).future
    }

  private def parsedRequest(searchList: Map[String, String], unitLinksData: JsValue, withPeriod: Option[String], limit: Option[Int] = None)(implicit ws: RequestGenerator): Future[Seq[JsValue]] = {
    val futures = searchList.flatMap {
      case (group, id) => lookupUnit(group, id, unitLinksData, withPeriod, limit)
    }.map { futResponse =>
      futResponse.map { resp =>
        LOGGER.debug(s"Result for record is: ${resp.body}")
        // @ TODO - add to success or failure to JSON ??
        resp.json
      }
    }.toSeq

    Future.sequence(futures)
  }

  private def toJson(record: (JsValue, JsValue), `type`: String, periodFromRowKey: String): JsValue = {
    Json.toJson(JsObject(record match {
      case (link, unit) =>

        // @ TODO PATCH - fix and remove patch
        // BI does not have period, so use an empty string
        val period = if (`type` == ENT.toString) {
          (link \ "period").getOrNull
        } else if (`type` == LOU.toString) {
          Json.toJson(periodFromRowKey)
        } else {
          (unit.as[JsValue] \ "period").getOrNull
        }

        // @ TODO PATCH - fix and remove patch when BI and ENTERPRISE apis are fixed
        // For BI, there is no "vars", just use the whole record
        //        val vars = if (`type` == ENT.toString || `type` == LEU.toString) {
        val vars = if (`type` == ENT.toString) {
          (unit \ "vars").getOrElse(unit)
        } else if (`type` == LOU.toString) {
          unit
        } else {
          (unit.as[JsValue] \ "variables").getOrNull
        }

        val unitType = DataSourceTypesUtil.fromString(`type`).getOrElse("").toString

        // Only return childrenJson with an Enterprise
        (unitType match {
          case "ENT" =>
            Json.obj(
              "id" -> (link \ "id").getOrNull,
              "parents" -> (link \ "parents").getOrNull,
              "children" -> (link \ "children").getOrNull,
              "childrenJson" -> (unit \ "childrenJson").getOrNull,
              "unitType" -> unitType,
              "period" -> period,
              "vars" -> vars
            )
          case _ =>
            Json.obj(
              "id" -> (link \ "id").getOrNull,
              "parents" -> (link \ "parents").getOrNull,
              "children" -> (link \ "children").getOrNull,
              "unitType" -> unitType,
              "period" -> period,
              "vars" -> vars
            )
        }).fields.filterNot { case (_, v) => v == JsNull }
    }))
  }

  private def lookupUnit(group: String, id: String, unitLinksData: JsValue, withPeriod: Option[String], limit: Option[Int])(implicit ws: RequestGenerator): Option[Future[WSResponse]] = {
    val unitOpt = DataSourceTypesUtil.fromString(group)
    if (unitOpt.isEmpty) logger.warn(s"Unrecognised group value [$group].")
    unitOpt.map { unit =>
      val path = apiUrlFor(unit)
      val newPath = unit match {
        case LOU => createLouUri(path, id, unitLinksData)
        case _ => createUri(path, id, withPeriod, group = unit.toString, history = limit)
      }
      LOGGER.info(s"Sending request to $newPath to get records of all variables of unit.")
      // @TODO - Duration.Inf -> place cap
      ws.singleGETRequestWithTimeout(newPath.toString, Duration.Inf)
    }
  }

  private def apiUrlFor(unit: DataSourceTypes): String =
    unit match {
      case LEU => LEGAL_UNIT_DATA_API_URL
      case CRN => CH_ADMIN_DATA_API_URL
      case VAT => VAT_ADMIN_DATA_API_URL
      case PAYE => PAYE_ADMIN_DATA_API_URL
      case ENT => SBR_CONTROL_API_URL
      case LOU => SBR_CONTROL_API_URL
    }

  private def responseException(implicit messagesProvider: MessagesProvider): PartialFunction[Throwable, Result] = {
    case ex: DateTimeParseException =>
      BadRequest(Messages("controller.datetime.failed.parse", ex.toString))
    case ex: RuntimeException =>
      InternalServerError(errAsJson(ex.toString, ex.getCause.toString))
    case ex: ServiceUnavailableException =>
      ServiceUnavailable(errAsJson(ex.toString, ex.getCause.toString))
    case ex: TimeoutException =>
      RequestTimeout(Messages("controller.timeout.request", s"$ex", s"${ex.getCause}"))
    case ex => InternalServerError(errAsJson(ex.toString, ex.getCause.toString))
  }
}

private object SearchController {
  private val CAPPED_DISPLAY_NUMBER = 1
  private val FIXED_YEARMONTH_SIZE = 6
}