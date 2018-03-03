package controllers.v1

import java.time.format.DateTimeParseException
import javax.naming.ServiceUnavailableException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{ Future, TimeoutException }

import play.api.i18n.{ I18nSupport, Messages }
import play.api.libs.json._
import play.api.mvc.{ Controller, Result }
import org.slf4j.{ Logger, LoggerFactory }
import com.netaporter.uri.Uri
import com.typesafe.scalalogging.StrictLogging

import uk.gov.ons.sbr.models._

import config.Properties
import utils.FutureResponse.futureSuccess
import utils.UriBuilder.createUri
import utils.Utilities.{ errAsJson, orElseNull }
import services.RequestGenerator

/**
 * ControllerUtils
 * ----------------
 * Author: haqa
 * Date: 10 July 2017 - 09:25
 * Copyright (c) 2017  Office for National Statistics
 */
// @todo - fix typedef
trait ControllerUtils extends Controller with StrictLogging with Properties with I18nSupport {

  protected val PLACEHOLDER_PERIOD = "*date"
  private val PLACEHOLDER_UNIT_TYPE = "*type"
  // number of units displayable
  private val CAPPED_DISPLAY_NUMBER = 1
  protected val FIXED_YEARMONTH_SIZE = 6

  protected[this] val LOGGER: Logger = LoggerFactory.getLogger(getClass.getName)

  protected type UnitLinksListType = Seq[JsValue]
  protected type StatisticalUnitLinkType = JsValue

  private def toJson(record: (JsValue, JsValue), `type`: String): JsValue = {
    Json.toJson(JsObject(record match {
      case (link, unit) =>

        // @ TODO PATCH - fix and remove patch
        // BI does not have period, so use an empty string
        val period = if (`type` == ENT.toString) {
          (unit \ "period").getOrNull
        } else {
          (unit.as[Seq[JsValue]].head \ "period").getOrNull
        }

        // @ TODO PATCH - fix and remove patch when BI and ENTERPRISE apis are fixed
        // For BI, there is no "vars", just use the whole record
        //        val vars = if (`type` == ENT.toString || `type` == LEU.toString) {
        val vars = if (`type` == ENT.toString) {
          (unit \ "vars").getOrElse(unit)
        } else {
          (unit.as[Seq[JsValue]].head \ "variables").getOrNull
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

  private def responseException: PartialFunction[Throwable, Result] = {
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

  // @ TODO - CHECK error control
  protected def search[T](key: String, baseUrl: Uri, sourceType: DataSourceTypes = ENT,
    periodParam: Option[String] = None, history: Option[Int] = None)(implicit
    fjs: Reads[T],
    ws: RequestGenerator): Future[Result] = {
    key match {
      case k if k.length >= MINIMUM_KEY_LENGTH =>
        LOGGER.debug(s"Sending request to ${baseUrl.toString} to retrieve Unit Links")
        ws.singleGETRequest(baseUrl.toString) map {
          case response if response.status == OK => {
            // @ TODO - add to success or failure to JSON ??
            val unitResp = response.json.as[T]
            unitResp match {
              case u: UnitLinksListType =>
                // if one UnitLinks found -> get unit
                if (u.length == CAPPED_DISPLAY_NUMBER) {
                  val id = (u.head \ "id").as[String]
                  LOGGER.debug(s"Found a single response with $id")
                  val mapOfRecordKeys = Map((u.head \ "unitType").as[String] -> id)
                  val respRecords = parsedRequest(mapOfRecordKeys, periodParam, history)
                  val json: Seq[JsValue] = (u zip respRecords).map(x => toJson(x, (u.head \ "unitType").as[String]))
                  Ok(Json.toJson(json)).as(JSON)
                } else {
                  LOGGER.debug(s"Found multiple records matching given id, $key. Returning multiple as list.")
                  // return UnitLinks if multiple
                  PartialContent(unitResp.toString).as(JSON)
                }
              case s: StatisticalUnitLinkType =>
                val mapOfRecordKeys = Map(sourceType.toString -> (s \ "id").as[String])
                val respRecords = parsedRequest(mapOfRecordKeys, periodParam)
                val json = (Seq(s) zip respRecords).map(x => toJson(x, sourceType.toString)).head
                Ok(json).as(JSON)
            }
          }
          case response if response.status == NOT_FOUND => NotFound(response.body).as(JSON)
        } recover responseException
      case _ =>
        BadRequest(Messages("controller.invalid.id", key, MINIMUM_KEY_LENGTH)).future
    }
  }

  private def parsedRequest(searchList: Map[String, String], withPeriod: Option[String] = None,
    limit: Option[Int] = None)(implicit ws: RequestGenerator): List[JsValue] = {
    searchList.map {
      case (group, id) =>
        val unit = DataSourceTypesUtil.fromString(group)
        val path = unit match {
          case Some(LEU) => LEGAL_UNIT_DATA_API_URL
          case Some(CRN) => CH_ADMIN_DATA_API_URL
          case Some(VAT) => VAT_ADMIN_DATA_API_URL
          case Some(PAYE) => PAYE_ADMIN_DATA_API_URL
          case Some(ENT) => SBR_CONTROL_API_URL
        }
        // TODO - fix unit.getOrElse("").toString
        val newPath = createUri(path, id, withPeriod, group = unit.getOrElse("").toString, history = limit)
        LOGGER.info(s"Sending request to $newPath to get records of all variables of unit.")
        // @TODO - Duration.Inf -> place cap
        val resp = ws.singleGETRequestWithTimeout(newPath.toString, Duration.Inf)
        // @ TODO - add to success or failrue to JSON ??
        resp.json
    }.toList
  }

}
