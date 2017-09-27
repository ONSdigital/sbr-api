package services.WSRequest

import javax.inject.{ Inject, Singleton }

import com.netaporter.uri.Uri
import org.slf4j.LoggerFactory

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import play.api.http.{ ContentTypes, Status }
import play.api.libs.json.JsValue
import play.api.libs.ws.{ WSClient, WSResponse }
import play.api.mvc.Results
import uk.gov.ons.sbr.models.UnitTypesShortNames._
import config.Properties._
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

  /**
   *
   * TODO - duration.inf -> place cap
   *
   */
  def parsedRequest(searchList: Map[String, String], withPeriod: Option[String] = None): List[JsValue] = {
    searchList.map {
      case (group, id) =>
        val path = group.toUpperCase match {
          case LEGAL_UNIT_TYPE => biBase
          case COMPANIES_HOUSE_REFERENCE_NUMBER_TYPE => sbrAdminBase
          case PAYE_TYPE => sbrAdminBase
          case VAT_REFERENCE_TYPE => sbrAdminBase
          case ENTERPRISE_TYPE => sbrControlApiBase
        }
        val newPath = uriPathBuilder(path, id, withPeriod, group = group.toLowerCase)
        logger.info(s"Sending request to $newPath")
        val resp = singleRequestWithTimeout(newPath.toString, Duration.Inf)
        resp.json
    }.toList
  }

}
