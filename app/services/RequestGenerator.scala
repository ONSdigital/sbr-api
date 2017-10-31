package services.WSRequest

import javax.inject.{ Inject, Singleton }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

import org.slf4j.LoggerFactory
import com.netaporter.uri.Uri
import play.api.http.{ ContentTypes, Status }
import play.api.libs.json.JsValue
import play.api.libs.ws.{ WSClient, WSResponse }
import play.api.mvc.Results

import uk.gov.ons.sbr.models._

import config.Properties.{ biBase, requestTimeout, sbrAdminBase, sbrControlApiBase }
import utils.UriBuilder.uriPathBuilder

/**
 * Created by haqa on 20/07/2017.
 */
@Singleton
class RequestGenerator @Inject() (ws: WSClient) extends Results with Status with ContentTypes {

  private[this] val logger = LoggerFactory.getLogger(getClass)

  //  @SuppressWarnings("Unused - for request debugging")
  private def status(response: WSResponse) = response.status

  def singleRequest(path: Uri): Future[WSResponse] =
    ws.url(path.toString).withRequestTimeout(requestTimeout.millis).get()

  private def singleRequestWithTimeout(url: String, timeout: Duration = Duration(requestTimeout, MILLISECONDS)) =
    Await.result(ws.url(url).get(), timeout)

  //  @SuppressWarnings("Unused - see HealthController")
  private def reroute(host: String, route: String) = {
    logger.debug(s"rerouting to search route $route")
    Redirect(url = s"http://$host/v1/searchBy$route").flashing(
      "redirect" -> s"You are being redirected to $route route", "status" -> "ok"
    )
  }

  def controlReroute(url: String, headers: (String, String), body: JsValue): Future[WSResponse] = {
    logger.debug(s"Rerouting to route: $url")
    ws.url(url).withHeaders(headers).withRequestTimeout(requestTimeout.millis).post(body)
  }

  /**
   *
   * TODO - duration.inf -> place cap
   *
   */
  def parsedRequest(searchList: Map[String, String], withPeriod: Option[String] = None): List[JsValue] = {
    searchList.map {
      case (group, id) =>
        // fix ch -> crn
        val filter = group match {
          case x if x == "CH" => "CRN"
          case x => x
        }
        val path = DataSourceTypesUtil.fromString(filter.toUpperCase) match {
          case Some(LEU) => biBase
          case Some(CRN | PAYE | VAT) => sbrAdminBase
          case Some(ENT) => sbrControlApiBase
        }
        val newPath = uriPathBuilder(path, id, withPeriod, group = filter)
        logger.info(s"Sending request to $newPath")
        val resp = singleRequestWithTimeout(newPath.toString, Duration.Inf)
        resp.json
    }.toList
  }

}
