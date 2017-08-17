package controllers.v1

import java.time.format.DateTimeParseException
import javax.naming.ServiceUnavailableException

import play.api.mvc.{ AnyContent, Controller, Request, Result }
import com.typesafe.scalalogging.StrictLogging
import play.api.libs.ws.WSClient
import utils.CsvProcessor.{ headerToSeq, readFile }
import utils.Utilities.errAsJson
import com.outworkers.util.play._
import play.api.libs.json.JsValue
import services.WSRequest.RequestGenerator

import scala.util.{ Failure, Success, Try }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future, TimeoutException }
import scala.concurrent.duration._
import uk.gov.ons.sbr.models.UnitTypes._
import config.Properties.{ minKeyLength, controlEndpoint, host }

/**
 * Created by haqa on 10/07/2017.
 */
trait ControllerUtils extends Controller with StrictLogging {

  /**
   *
   * takes map only
   * then returns string of type
   * then use string and concat default serachBy url
   */
  protected def unitMatch(m: Map[String, String]): Iterable[String] =
    m.map {
      case x if x._1 == LegalUnitType =>
        LegalUnitType
      case x if x._1 == EnterpriseUnitType =>
        EnterpriseUnitType
      case x if x._1 == PayAsYouEarnUnitType =>
        PayAsYouEarnUnitType
      case x if x._1 == ValueAddedTaxUnitType =>
        ValueAddedTaxUnitType
      case x if x._1 == CompanyRegistrationNumberUnitType =>
        CompanyRegistrationNumberUnitType
    }

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

  //
  //  /**
  //    * @note - simplify - AnyRef rep with t.param X
  //    *
  //    * @param v - value param to convert
  //    * @param f - scala conversion function
  //    * @param msg - overriding msg option
  //    * @tparam Z - java data type for value param
  //    * @return Future[Result]
  //    */
  //  protected def resultMatcher[Z](v: Optional[Z], f: Optional[Z] => AnyRef,
  //                                 msg: Option[String] = None): Future[Result] = {
  //    Future { f(v) }.map {
  //      case Some(x: List[StatisticalUnit]) => tryAsResponse[List[StatisticalUnit]](Links.toJson, x)
  //      case Some(x: Enterprise) => tryAsResponse[Enterprise](EnterpriseKey.toJson, x)
  //      case None =>
  //        BadRequest(errAsJson(BAD_REQUEST, "bad_request", msg.getOrElse("Could not parse returned response")))
  //    }
  //  }

  /**
   *
   * @todo - add error control for try[parse, mapping]
   *       - modulate ws func
   *       - result - future?
   *       - new type -> unitName
   */
  //  def sendRequest(ws: WSClient, url: String, toJson: String => JsValue): Future[Result] = {
  //    val res = ws.url(url).withRequestTimeout(requestTimeout.millis).get().map {
  //      response =>
  //        println(response)
  //        val js = toJson(response.json.as[Seq[JsValue]].map(x => x).mkString);
  //        //        val vat = js.as[JsObject] + ("source" -> JsString("VAT"));
  //        Ok(js).as(JSON)
  //    }.recover {
  //      case t: TimeoutException =>
  //        RequestTimeout(errAsJson(REQUEST_TIMEOUT, "request_timeout", "This may be due to connection being blocked."))
  //      case e: ServiceUnavailableException =>
  //        ServiceUnavailable(errAsJson(
  //          SERVICE_UNAVAILABLE, "service_unavailable", "Cannot Connect to host. Please verify the address is correct."
  //        ))
  //      //      case _ =>
  //      //        BadRequest(errAsJson(NOT_FOUND, "bad_request", "Cannot find specified id."))
  //    }
  //    res
  //  }

}
