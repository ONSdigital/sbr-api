package services.WSRequest

import javax.inject.Inject

import controllers.v1.{ ControllerUtils }
import play.api.libs.ws.WSClient
import play.api.mvc.Result
import utils.Utilities.errAsJson

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future, TimeoutException }
import scala.concurrent.duration._

/**
 * Created by haqa on 20/07/2017.
 */
class WSRequest @Inject() (ws: WSClient) extends ControllerUtils {

  def sendRequest(url: String): Future[Result] = {
    val res = ws.url(url).withRequestTimeout(5000.millis).get().map {
      response =>
        Ok(response.body).as(JSON)
    } recover {
      case t: TimeoutException =>
        RequestTimeout(errAsJson(408, "request_timeout", "This may be due to connection being blocked."))
      case e =>
        ServiceUnavailable(errAsJson(503, "service_unavailable", "Cannot Connect to host. Please verify the address is correct."))
    }
    //    ws.close()
    res
  }

}
