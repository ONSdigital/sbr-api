package controllers.v1

import java.time.format.DateTimeParseException
import javax.naming.ServiceUnavailableException

import scala.concurrent.TimeoutException

import com.typesafe.scalalogging.StrictLogging
import play.api.libs.json.{ JsDefined, JsUndefined, JsValue, Json }
import play.api.mvc.{ Controller, Result }

import utils.Utilities.{ errAsJson, orElseNull }

/**
 * Created by haqa on 10/07/2017.
 */
// @todo - fix typedef
trait ControllerUtils extends Controller with StrictLogging {

  protected val placeholderPeriod = "*date"
  protected val placeholderUnitType = "*type"

  // number of units displayable
  protected val cappedDisplayNumber = 1
  protected val fixedYeaMonthSize = 6

  protected def toJson(record: (JsValue, JsValue)): JsValue = {
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

  protected def responseException: PartialFunction[Throwable, Result] = {
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

}
