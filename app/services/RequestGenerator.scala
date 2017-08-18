package services.WSRequest

import javax.inject.{ Inject, Singleton }
import javax.naming.ServiceUnavailableException

import play.api.http.{ ContentTypes, Status }
import play.api.libs.json.JsValue
import play.api.libs.ws.{ WSClient, WSRequest, WSResponse }
import play.api.mvc.{ Result, Results }
import utils.Utilities.errAsJson
import config.Properties.{ baseSearchRoute, controlEndpoint, requestTimeout }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import org.slf4j.LoggerFactory

/**
 * Created by haqa on 20/07/2017.
 */
@Singleton
class RequestGenerator @Inject() (ws: WSClient) extends Results with Status with ContentTypes {

  private[this] val logger = LoggerFactory.getLogger(getClass)

  protected def status(response: WSResponse) = response.status

  @deprecated("Migrated to singleRequest", "feature/config-port [Thu 17 Aug 2017 - 14:30]")
  protected def sendRequest(url: String): Future[Result] = {
    val res = ws.url(url).withRequestTimeout(requestTimeout.millis).get().map {
      response =>
        if (response.status == 200) Ok(response.body).as(JSON)
        else NotFound(response.body).as(JSON)
    }
    //    ws.close()
    res
  }

  def singleRequest(id: String, prefix: String = controlEndpoint): Future[WSResponse] = {
    val res = ws.url(s"$prefix$id").withRequestTimeout(requestTimeout.millis).get()
    res
  }

  def singleRequestNoTimeout(url: String): Future[WSResponse] = ws.url(url).get()

  def reroute(host: String, route: String) = {
    logger.debug(s"rerouting to search route $route")
    Redirect(url = s"http://$host/v1/searchBy$route").flashing(
      "redirect" -> s"You are being redirected to $route route", "status" -> "ok"
    )
  }

  /**
   * @note searchList - change param type
   */
  def multiRequest(searchList: Map[String, String], prefix: String = baseSearchRoute, suffix: String = "s/"): List[JsValue] = {
    searchList.map { s =>
      val id = s._2
      val path = s._1.toLowerCase
      val resp = Await.result(singleRequestNoTimeout(s"$prefix$path$suffix$id"), Duration.Inf)
      resp.json
    }.toList
  }

}
