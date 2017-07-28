package services.WSRequest

import javax.inject.Inject
import javax.naming.ServiceUnavailableException

import controllers.v1.ControllerUtils
import play.api.libs.ws.WSClient
import play.api.mvc.Result
import utils.Utilities.errAsJson

import utils.Properties.{ requestTimeout }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future, TimeoutException }
import scala.concurrent.duration._

/**
 * Created by haqa on 20/07/2017.
 */
class WSRequest @Inject() (ws: WSClient) extends ControllerUtils {

  def sendRequest(url: String): Future[Result] = {
    val res = ws.url(url).withRequestTimeout(requestTimeout.millis).get().map {
      response =>
        Ok(response.body).as(JSON)
    } recover {
      case (t: TimeoutException) =>
        RequestTimeout(errAsJson(REQUEST_TIMEOUT, "request_timeout", "This may be due to connection being blocked."))
      case (e: ServiceUnavailableException) =>
        ServiceUnavailable(errAsJson(SERVICE_UNAVAILABLE, "service_unavailable", "Cannot Connect to host. Please verify the address is correct."))
      case _ =>
        BadRequest(errAsJson(NOT_FOUND, "bad_request", "Cannot find specified id."))
    }
    //    ws.close()
    res
  }

}
