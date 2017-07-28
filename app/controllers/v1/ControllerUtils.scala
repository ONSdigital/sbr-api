package controllers.v1

import javax.naming.ServiceUnavailableException

import play.api.mvc.{ AnyContent, Controller, Request, Result }
import com.typesafe.scalalogging.StrictLogging
import models.units.Enterprise
import play.api.libs.ws.WSClient
import utils.CsvProcessor.{ headerToSeq, readFile, sampleFile }
import utils.Utilities.errAsJson

import utils.Properties.requestTimeout
import scala.util.{ Failure, Success, Try }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future, TimeoutException }
import scala.concurrent.duration._

/**
 * Created by haqa on 10/07/2017.
 */
trait ControllerUtils extends Controller with StrictLogging {

  private val idIndex = 1

  protected def getQueryString(request: Request[AnyContent]) = request.queryString.map { case v => v._2.mkString }

  protected[this] def errAsResponse(f: => Future[Result]): Future[Result] = Try(f) match {
    case Success(g) => g
    case Failure(err) =>
      logger.error("Unable to produce response.", err)
      Future.successful {
        InternalServerError(s"{err = '${err}'}")
      }
  }

  def findRecord(element: String, filename: String): List[Enterprise] = {
    val headers = headerToSeq(sampleFile)
    val records = for {
      data <- readFile(filename)
      cols = data.split(",").map(_.trim)
      res: Option[Enterprise] = if (cols(idIndex).matches(element)) {
        logger.info(s"Found matching record with ${element} " +
          s"as data[${cols(cols.indexOf(element))}] identified as ${cols(cols.indexOf(element))} type")
        val rec = (headers zip cols).toMap
        Some(Enterprise.fromMap(rec))
      } else {
        None
      }
    } yield (res)
    records.flatten.toList
  }

  def sendRequest(ws: WSClient, url: String): Future[Result] = {
    val res = ws.url(url).withRequestTimeout(requestTimeout.millis).get().map {
      response => Ok(response.body).as(JSON)
    }
    res.recover {
      case (t: TimeoutException) =>
        RequestTimeout(errAsJson(REQUEST_TIMEOUT, "request_timeout", "This may be due to connection being blocked."))
      case (e: ServiceUnavailableException) =>
        ServiceUnavailable(errAsJson(SERVICE_UNAVAILABLE, "service_unavailable", "Cannot Connect to host. Please verify the address is correct."))
      case _ =>
        BadRequest(errAsJson(NOT_FOUND, "bad_request", "Cannot find specified id."))
    }
    res
  }

}
