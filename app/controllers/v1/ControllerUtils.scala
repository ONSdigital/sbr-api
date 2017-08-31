package controllers.v1

import java.time.format.DateTimeParseException
import javax.naming.ServiceUnavailableException

import play.api.mvc.{ Controller, Result }
import com.typesafe.scalalogging.StrictLogging
import play.api.libs.json.{ JsValue, Json }
import utils.Utilities.{errAsJson, orElseNull}

import scala.concurrent.TimeoutException

/**
 * Created by haqa on 10/07/2017.
 */
trait ControllerUtils extends Controller with StrictLogging {

  protected val placeholderPeriod = "*date"
  // number of units displayable
  protected val cappedDisplayNumber = 1
  protected val fixedYeaMonthSize = 6

  protected def toJson(record: Seq[JsValue], links: Seq[JsValue]): JsValue = {
    val res = (links zip record).map(
      z => {
        // For BI, there is no "vars", just use the whole record
        val vars = (z._2 \ "vars").getOrElse(z._2)
        // BI does not have period, so use an empty string
        val period = (z._2 \ "period").getOrNull

        val js = Json.obj(
          "id" -> (z._1 \ "id").getOrNull,
          "parents" -> (z._1 \ "parents").getOrNull,
          "children" -> (z._1 \ "children").getOrNull,
          "unitType" -> (z._1 \ "unitType").getOrNull,
          "period" -> period,
          "vars" -> vars
        )
        js
      }
    )
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
