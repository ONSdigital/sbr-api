package controllers.v1

import javax.naming.ServiceUnavailableException

import play.api.mvc.{ AnyContent, Controller, Request, Result }
import com.typesafe.scalalogging.StrictLogging
import play.api.libs.ws.WSClient
import utils.CsvProcessor.{ headerToSeq, readFile }
import utils.Utilities.errAsJson
import com.outworkers.util.play._
import play.api.libs.json.JsValue
import utils.Properties.requestTimeout

import scala.util.{ Failure, Success, Try }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future, TimeoutException }
import scala.concurrent.duration._
import utils.Properties._

/**
 * Created by haqa on 10/07/2017.
 */
trait ControllerUtils extends Controller with StrictLogging {

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

  def sendRequest(ws: WSClient, url: String): Future[Result] = {
    val res = ws.url(url).withRequestTimeout(requestTimeout.millis).get().map {
      response => Ok(response.body).as(JSON)
    }.recover {
      case t: TimeoutException =>
        RequestTimeout(errAsJson(REQUEST_TIMEOUT, "request_timeout", "This may be due to connection being blocked."))
      case e: ServiceUnavailableException =>
        ServiceUnavailable(errAsJson(SERVICE_UNAVAILABLE, "service_unavailable", "Cannot Connect to host. Please verify the address is correct."))
      case _ =>
        BadRequest(errAsJson(NOT_FOUND, "bad_request", "Cannot find specified id."))
    }
    res
  }

}
