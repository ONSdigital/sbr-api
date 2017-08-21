package controllers.v1

import java.time.format.DateTimeParseException
import javax.naming.ServiceUnavailableException

import play.api.mvc.{ AnyContent, Controller, Request, Result }
import com.typesafe.scalalogging.StrictLogging
import utils.Utilities.errAsJson
import play.api.libs.json.JsValue

import scala.annotation.tailrec
import scala.util.{ Failure, Success, Try }
import scala.concurrent.{ Future, TimeoutException }

/**
 * Created by haqa on 10/07/2017.
 */
trait ControllerUtils extends Controller with StrictLogging {

  protected val placeholderPeriod = "date"
  // number of units displayable
  protected val cappedDisplayNumber = 1

  //  @tailrec
  //  protected def getRootCause ()
  //
  //  @tailrec
  //  protected def errLogBuilder (x: Throwable, msgSeq: Vector[String] = Nil)  = {
  //
  //  }

  // @todo - add getCause -> root
  protected def responseException: PartialFunction[Throwable, Result] = {
    case ex: DateTimeParseException =>
      BadRequest(errAsJson(BAD_REQUEST, "invalid_date", s"cannot parse date exception found $ex"))
    case ex: RuntimeException => InternalServerError(errAsJson(INTERNAL_SERVER_ERROR, "runtime_exception", s"$ex", s"$ex"))
    case ex: ServiceUnavailableException =>
      ServiceUnavailable(errAsJson(SERVICE_UNAVAILABLE, "service_unavailable", s"$ex", s"$ex"))
    case ex: TimeoutException =>
      RequestTimeout(errAsJson(REQUEST_TIMEOUT, "request_timeout", s"$ex", s"This may be due to connection being blocked or host failure. Found exception $ex"))
    case ex => InternalServerError(errAsJson(INTERNAL_SERVER_ERROR, "internal_server_error", s"$ex"))
  }

}
