package services

import com.typesafe.config.{ Config, ConfigFactory }
import config.Properties
import javax.inject.{ Inject, Singleton }
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.http.{ ContentTypes, Status }
import play.api.libs.json.JsValue
import play.api.libs.ws.{ WSClient, WSResponse }
import play.api.mvc.{ Result, Results }

import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class RequestGenerator @Inject() (
    ws: WSClient,
    val configuration: Configuration
) extends Results with Status with ContentTypes with Properties {

  private[this] val LOGGER = LoggerFactory.getLogger(getClass)

  private final val config: Config = ConfigFactory.load()
  private final val DURATION_METRIC: TimeUnit = MILLISECONDS
  private final val TIMEOUT_REQUEST: Long = config.getString("request.timeout").toLong

  private val TIME_UNIT_COLLECTION: List[TimeUnit] =
    List(NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS)

  final def timeUnitMapper(s: String): TimeUnit =
    TIME_UNIT_COLLECTION.find(_.toString.equalsIgnoreCase(s))
      .getOrElse(throw new IllegalArgumentException(s"Could not find TimeUnit + $s"))

  def reroute(host: String, route: String): Result = {
    LOGGER.debug(s"rerouting to search route $route")
    Redirect(url = s"http://$host/v1/searchBy$route")
      .flashing("redirect" -> s"You are being redirected to $route route", "status" -> "ok")
  }

  def singleGETRequest(path: String, headers: Seq[(String, String)] = Seq.empty, params: Seq[(String, String)] = Seq.empty): Future[WSResponse] =
    ws.url(path.toString)
      .withQueryString(params: _*)
      .withHeaders(headers: _*)
      .withRequestTimeout(Duration(TIMEOUT_REQUEST, DURATION_METRIC))
      .get

  def singleGETRequestWithTimeout(url: String, timeout: Duration = Duration(TIMEOUT_REQUEST, DURATION_METRIC)): Future[WSResponse] =
    ws.url(url.toString)
      .withRequestTimeout(timeout)
      .get()

  def singlePOSTRequest(url: String, headers: (String, String), body: JsValue): Future[WSResponse] = {
    LOGGER.debug(s"Rerouting to route: $url")
    ws.url(url.toString)
      .withHeaders(headers)
      .withRequestTimeout(Duration(TIMEOUT_REQUEST, DURATION_METRIC))
      .post(body)
  }
}