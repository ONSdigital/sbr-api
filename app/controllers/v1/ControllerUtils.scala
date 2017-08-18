package controllers.v1

import java.time.format.DateTimeParseException
import javax.naming.ServiceUnavailableException

import play.api.mvc.{ AnyContent, Controller, Request, Result }
import com.typesafe.scalalogging.StrictLogging
import utils.CsvProcessor.{ headerToSeq, readFile }
import utils.Utilities.errAsJson
import com.outworkers.util.play._
import play.api.libs.json.JsValue

import scala.util.{ Failure, Success, Try }
import scala.concurrent.{ Future, TimeoutException }
import config.Properties.minKeyLength

/**
 * Created by haqa on 10/07/2017.
 */
trait ControllerUtils extends Controller with StrictLogging {

  protected val placeholderPeriod = "date/"

  protected def retrieveRecord[T](key: String, filePath: String, fromMap: Map[String, String] => T,
    toJson: List[T] => JsValue): Future[Result] = {
    val res = key match {
      case key if key.length >= minKeyLength => findRecord[T](key, filePath, fromMap) match {
        case Nil =>
          logger.debug(s"No record found for id: $key")
          NotFound(errAsJson(NOT_FOUND, "not found", s"Could not find value $key")).future
        case x => Ok(toJson(x)).as(JSON).future
      }
      case _ => BadRequest(errAsJson(BAD_REQUEST, "missing parameter", "Not a valid key length")).future
    }
    res
  }

  //  @deprecated("Encapsulated in searchById", "demo/basic-search [Tue 1 Aug 2017 - 11:07]")
  protected def getQueryString(request: Request[AnyContent], elem: String): String = request.getQueryString(elem).getOrElse("")

  protected[this] def errAsResponse(f: => Future[Result]): Future[Result] = Try(f) match {
    case Success(g) => g
    case Failure(err) =>
      logger.error("Unable to produce response.", err)
      Future.successful {
        InternalServerError(s"{err = '${err}'}")
      }
  }

  protected[this] def tryAsResponse[T](f: T => JsValue, v: T): Result = Try(f(v)) match {
    case Success(s) => Ok(s)
    case Failure(ex) =>
      logger.error("Failed to parse instance to expected json format", ex)
      //      Future.successful {
      BadRequest(errAsJson(BAD_REQUEST, "bad_request", s"Could not perform action ${f.toString} with exception $ex"))
    //  }
  }

  def findRecord[T](element: String, filename: String, f: Map[String, String] => T): List[T] = {
    val headers = headerToSeq(filename)
    val records = for {
      data <- readFile(filename)
      cols = data.split(",").map(_.trim)
      res: Option[T] = if (cols.contains(element)) {
        logger.info(s"Found matching record with ${element} " +
          s"as data[${cols(cols.indexOf(element))}] identified as ${cols(cols.indexOf(element))} type")
        Some(f((headers zip cols).toMap))
      } else {
        None
      }
    } yield (res)
    records.flatten.toList
  }

  protected def responseException: PartialFunction[Throwable, Result] = {
    case ex: DateTimeParseException =>
      BadRequest(errAsJson(BAD_REQUEST, "invalid_date", s"cannot parse date exception found $ex"))
    case ex: RuntimeException => InternalServerError(errAsJson(INTERNAL_SERVER_ERROR, "runtime_exception", s"$ex"))
    case ex: ServiceUnavailableException =>
      ServiceUnavailable(errAsJson(SERVICE_UNAVAILABLE, "service_unavailable", s"$ex"))
    case ex: TimeoutException =>
      RequestTimeout(errAsJson(REQUEST_TIMEOUT, "request_timeout", s"This may be due to connection being blocked or host failure. Found exception $ex"))
    case ex => InternalServerError(errAsJson(INTERNAL_SERVER_ERROR, "internal_server_error", s"$ex"))
  }


}
