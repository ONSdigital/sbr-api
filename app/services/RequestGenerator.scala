package services

import javax.inject.{ Inject, Singleton }

import scala.concurrent.duration.Duration.Infinite
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

import play.api.Configuration
import play.api.http.{ ContentTypes, Status }
import play.api.libs.json.JsValue
import play.api.libs.ws.{ WSClient, WSResponse }
import play.api.mvc.{ Result, Results }
import org.slf4j.LoggerFactory
import com.typesafe.config.{ Config, ConfigFactory }

import config.Properties

/**
 * WSRequestGenerator
 * ----------------
 * Author: haqa
 * Date: 16 November 2017 - 09:25
 * Copyright (c) 2017  Office for National Statistics
 */

@Singleton
class RequestGenerator @Inject() (
    ws: WSClient,
    val configuration: Configuration
//    durationMetric: TimeUnit = MILLISECONDS,
//    timeout: Option[Long] = None
) extends Results with Status with ContentTypes with Properties {

  private[this] val LOGGER = LoggerFactory.getLogger(getClass)

  private final val config: Config = ConfigFactory.load()
  private final val DURATION_METRIC: TimeUnit = MILLISECONDS
  private final val TIMEOUT_REQUEST: Long = config.getString("request.timeout").toLong
  private final val INF_REQUEST: Infinite = Duration.Inf

  private val TimeUnitCollection: List[TimeUnit] =
    List(NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS)

  final def timeUnitMapper(s: String): TimeUnit =
    TimeUnitCollection.find(_.toString.equalsIgnoreCase(s))
      .getOrElse(throw new IllegalArgumentException(s"Could not find TimeUnit + $s"))

  def reroute(host: String, route: String): Result = {
    LOGGER.debug(s"rerouting to search route $route")
    Redirect(url = s"http://$host/v1/searchBy$route")
      .flashing("redirect" -> s"You are being redirected to $route route", "status" -> "ok")
  }

  def singleGETRequest(path: String, headers: Seq[(String, String)] = Seq(), params: Seq[(String, String)] = Seq()): Future[WSResponse] =
    ws.url(path.toString)
      .withQueryString(params: _*)
      .withHeaders(headers: _*)
      .withRequestTimeout(Duration(TIMEOUT_REQUEST, DURATION_METRIC))
      .get

  def singleGETRequestWithTimeout(url: String, timeout: Duration = Duration(TIMEOUT_REQUEST, DURATION_METRIC)): WSResponse =
    Await.result(ws.url(url.toString).get(), timeout)

  @deprecated("Migrate to singlePOSTRequest", "27 Nov 2017 - fix/tidy-up-1")
  def controlReroute(url: String, headers: (String, String), body: JsValue): Future[WSResponse] = {
    LOGGER.debug(s"Rerouting to route: $url")
    ws.url(url).withHeaders(headers).withRequestTimeout(requestTimeout.millis).post(body)
  }

  def singlePOSTRequest(url: String, headers: (String, String), body: JsValue): Future[WSResponse] = {
    LOGGER.debug(s"Rerouting to route: $url")
    ws.url(url.toString)
      .withHeaders(headers)
      .withRequestTimeout(Duration(TIMEOUT_REQUEST, DURATION_METRIC))
      .post(body)
  }
}