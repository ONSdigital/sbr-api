package repository.rest

import java.util.concurrent.TimeoutException

import com.typesafe.scalalogging.LazyLogging
import javax.inject.Inject
import play.api.http.Status.{ NOT_FOUND, OK }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsValue
import play.api.libs.ws.{ WSClient, WSRequest, WSResponse }
import play.mvc.Http.HeaderNames.ACCEPT
import play.mvc.Http.MimeTypes.JSON
import repository.ErrorMessage
import utils.TrySupport
import utils.url.{ BaseUrl, Url }

import scala.concurrent.Future
import scala.util.Try

case class RestRepositoryConfig(baseUrl: BaseUrl)

class RestRepository @Inject() (config: RestRepositoryConfig, wsClient: WSClient) extends Repository with LazyLogging {

  override def getJson(path: String): Future[Either[ErrorMessage, Option[JsValue]]] = {
    val url = Url(withBase = config.baseUrl, withPath = path)
    requestFor(url).get().map {
      fromResponseToErrorOrJson
    }.recover(withTranslationOfFailureToError)
  }

  private def requestFor(url: String): WSRequest =
    wsClient.
      url(url).
      withHeaders(ACCEPT -> JSON)

  private def fromResponseToErrorOrJson(response: WSResponse): Either[ErrorMessage, Option[JsValue]] =
    response.status match {
      case OK => bodyAsJson(response).right.map(Some(_))
      case NOT_FOUND => Right(None)
      case _ => Left(describeStatus(response))
    }

  private def bodyAsJson(response: WSResponse): Either[ErrorMessage, JsValue] =
    TrySupport.fold(Try(response.json))(
      err => Left(s"Unable to create JsValue from unit response [${err.getMessage}]"),
      json => Right(json)
    )

  private def describeStatus(response: WSResponse): String =
    s"${response.statusText} (${response.status})"

  private def withTranslationOfFailureToError[B] = new PartialFunction[Throwable, Either[ErrorMessage, B]] {
    override def isDefinedAt(cause: Throwable): Boolean = true

    override def apply(cause: Throwable): Either[ErrorMessage, B] = {
      logger.error(s"Translating unit request failure [$cause].")
      cause match {
        case t: TimeoutException => Left(s"Timeout.  ${t.getMessage}")
        case t: Throwable => Left(t.getMessage)
      }
    }
  }
}
