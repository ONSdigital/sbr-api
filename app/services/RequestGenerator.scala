package services

import javax.inject.{ Inject, Singleton }

import scala.concurrent.duration.Duration.Infinite
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

import play.api.http.{ ContentTypes, Status }
import play.api.libs.json.JsValue
import play.api.libs.ws.{ WSClient, WSResponse }
import play.api.mvc.Results
import org.slf4j.LoggerFactory

import config.Properties.requestTimeout

/**
 * WSRequestGenerator
 * ----------------
 * Author: haqa
 * Date: 16 November 2017 - 09:25
 * Copyright (c) 2017  Office for National Statistics
 */

//@TODO - FIx -> use class args rather than override values
/**
 *
 * @tparam Z - type String or URI or uri path
 */
@Singleton
class RequestGenerator[Z] @Inject() (
    //    durationMetric: TimeUnit = MILLISECONDS,
    //    timeout: Option[Long] = None,
    ws: WSClient
) extends Results with Status with ContentTypes {

  private[this] val LOGGER = LoggerFactory.getLogger(getClass)
  /**
   * TEMPORARY SOLUTION - CLASS ARGS
   */
  private val durationMetric: TimeUnit = MILLISECONDS
  private val timeout: Option[Long] = None

  private final val DURATION_METRIC: TimeUnit = durationMetric
  private final val EXT_REQUEST_TIMEOUT: Long = timeout.getOrElse(requestTimeout)
  private final val INF_REQUEST: Infinite = Duration.Inf

  private val TimeUnitCollection: List[TimeUnit] =
    List(NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS)

  final def timeUnitMapper(s: String): TimeUnit =
    TimeUnitCollection.find(_.toString.equalsIgnoreCase(s))
      .getOrElse(throw new IllegalArgumentException(s"Could not find TimeUnit + $s"))

  def reroute(host: String, route: String) = {
    LOGGER.debug(s"rerouting to search route $route")
    Redirect(url = s"http://$host/v1/searchBy$route")
      .flashing("redirect" -> s"You are being redirected to $route route", "status" -> "ok")
  }

  //  def singleRawGETRequest(url: Z, headers: (String, String)*, queryString: (String, String)*): Future[WSResponse] =
  //    ws.url(url.toString)
  //      .withHeaders(headers)
  //      .withQueryString(queryString)
  //      .withRequestTimeout(Duration(EXT_REQUEST_TIMEOUT, DURATION_METRIC)).get

  final def singleGETRequest(path: Z): Future[WSResponse] =
    ws.url(path.toString)
      .withRequestTimeout(requestTimeout.millis).get()

  final def singleGETRequestWithTimeout(url: Z, timeout: Duration = Duration(EXT_REQUEST_TIMEOUT, DURATION_METRIC)) =
    Await.result(ws.url(url.toString).get(), timeout)

  @deprecated("Migrate to singlePOSTRequest", "27 Nov 2017 - fix/tidy-up-1")
  def controlReroute(url: String, headers: (String, String), body: JsValue): Future[WSResponse] = {
    LOGGER.debug(s"Rerouting to route: $url")
    ws.url(url).withHeaders(headers).withRequestTimeout(requestTimeout.millis).post(body)
  }

  def singlePOSTRequest(url: Z, headers: (String, String), body: JsValue): Future[WSResponse] = {
    LOGGER.debug(s"Rerouting to route: $url")
    ws.url(url.toString)
      .withHeaders(headers)
      .withRequestTimeout(Duration(EXT_REQUEST_TIMEOUT, DURATION_METRIC))
      .post(body)
  }

}