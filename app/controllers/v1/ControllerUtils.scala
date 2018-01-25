package controllers.v1

import java.time.format.DateTimeParseException
import javax.naming.ServiceUnavailableException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{ Future, TimeoutException }

import play.api.libs.json._
import play.api.mvc.{ Controller, Result }
import com.netaporter.uri.Uri
import com.typesafe.scalalogging.StrictLogging

import uk.gov.ons.sbr.models._

import config.Properties
import utils.FutureResponse.futureSuccess
import utils.UriBuilder.uriPathBuilder
import utils.Utilities.{ errAsJson, orElseNull }
import services.RequestGenerator

/**
 * SearchController
 * ----------------
 * Author: haqa
 * Date: 10 July 2017 - 09:25
 * Copyright (c) 2017  Office for National Statistics
 */
// @todo - fix typedef
trait ControllerUtils extends Controller with StrictLogging with Properties {

  protected val placeholderPeriod = "*date"
  private val placeholderUnitType = "*type"

  // number of units displayable
  private val cappedDisplayNumber = 1
  protected val fixedYeaMonthSize = 6

  protected type UnitLinksListType = Seq[JsValue]
  protected type StatisticalUnitLinkType = JsValue

  private def toJson(record: (JsValue, JsValue)): JsValue = {
    val res = record match {
      case (link, unit) => {
        // For BI, there is no "vars", just use the whole record
        val vars = (unit \ "vars").getOrElse(unit)
        // BI does not have period, so use an empty string
        val period = (unit \ "period").getOrNull

        // BI links do not have unitType
        val unitType = unit \ "unitType" match {
          case (v: JsDefined) => v.get.as[String]
          case (_: JsUndefined) => "LEU"
        }

        // Only return childrenJson with an Enterprise
        val js = unitType match {
          case "ENT" => {
            Json.obj(
              "id" -> (link \ "id").getOrNull,
              "parents" -> (link \ "parents").getOrNull,
              "children" -> (link \ "children").getOrNull,
              "childrenJson" -> (unit \ "childrenJson").getOrNull,
              "unitType" -> (unit \ "unitType").getOrNull,
              "period" -> period,
              "vars" -> vars
            )
          }
          case _ => {
            Json.obj(
              "id" -> (link \ "id").getOrNull,
              "parents" -> (link \ "parents").getOrNull,
              "children" -> (link \ "children").getOrNull,
              "unitType" -> unitType,
              "period" -> period,
              "vars" -> vars
            )
          }
        }
        js
      }
    }
    Json.toJson(res)
  }

  private def responseException: PartialFunction[Throwable, Result] = {
    case ex: DateTimeParseException =>
      BadRequest(errAsJson(BAD_REQUEST, "invalid_date", s"cannot parse date exception found $ex"))
    case ex: RuntimeException =>
      InternalServerError(errAsJson(INTERNAL_SERVER_ERROR, "runtime_exception", s"$ex", s"${ex.getCause}"))
    case ex: ServiceUnavailableException =>
      ServiceUnavailable(errAsJson(SERVICE_UNAVAILABLE, "service_unavailable", s"$ex", s"${ex.getCause}"))
    case ex: TimeoutException =>
      RequestTimeout(errAsJson(REQUEST_TIMEOUT, "request_timeout",
        s"This may be due to connection being blocked or host failure. Found exception $ex", s"${ex.getCause}"))
    case ex => InternalServerError(errAsJson(INTERNAL_SERVER_ERROR, "internal_server_error", s"$ex", s"${ex.getCause}"))
  }

  // @ TODO - CHECK error control
  protected def search[T](key: String, baseUrl: Uri, sourceType: DataSourceTypes = ENT,
    periodParam: Option[String] = None)(implicit fjs: Reads[T], ws: RequestGenerator): Future[Result] = {
    val res: Future[Result] = key match {
      case k if k.length >= minKeyLength =>
        ws.singleGETRequest(baseUrl.toString) map {
          case response if response.status == OK => {
            val unitResp = response.json.as[T]
            unitResp match {
              case u: UnitLinksListType =>
                // if one UnitLinks found -> get unit
                if (u.length == cappedDisplayNumber) {
                  val mapOfRecordKeys = Map((u.head \ "unitType").as[String] -> (u.head \ "id").as[String])
                  val respRecords = parsedRequest(mapOfRecordKeys, periodParam)
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
  private def parsedRequest(searchList: Map[String, String], withPeriod: Option[String] = None)(implicit ws: RequestGenerator): List[JsValue] = {
    searchList.map {
      case (group, id) =>
        // fix ch -> crn
        val filter = group match {
          case x if x == "CH" => "CRN"
          case x => x
        }
        val path = DataSourceTypesUtil.fromString(filter.toUpperCase) match {
          case Some(LEU) => businessIndexApiURL
          case Some(CRN) => chAdminDataApiURL
          case Some(VAT) => vatAdminDataApiURL
          case Some(PAYE) => payeAdminDataApiURL
          case Some(ENT) => sbrControlApiURL
        }
        val newPath = uriPathBuilder(path, id, withPeriod, group = filter)
        logger.info(s"Sending request to $newPath")
        val resp = ws.singleGETRequestWithTimeout(newPath.toString, Duration.Inf)
        resp.json
    }.toList
  }

}
