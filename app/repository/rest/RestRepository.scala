package repository.rest

import java.util.concurrent.TimeoutException

import com.typesafe.scalalogging.LazyLogging
import javax.inject.Inject
import play.api.http.Status.{ NOT_FOUND, OK }
import play.api.libs.json.JsValue
import play.api.libs.ws.{ WSRequest, WSResponse }
import play.mvc.Http.HeaderNames.ACCEPT
import play.mvc.Http.MimeTypes.JSON
import repository.ErrorMessage
import tracing.{ TraceData, TraceWSClient }
import utils.TrySupport
import utils.url.{ BaseUrl, Url }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

case class RestRepositoryConfig(baseUrl: BaseUrl)

class RestRepository @Inject() (config: RestRepositoryConfig, wsClient: TraceWSClient, ec: ExecutionContext) extends Repository with LazyLogging {

  override def getJson(path: String, spanName: String, traceData: TraceData): Future[Either[ErrorMessage, Option[JsValue]]] = {
    val url = Url(withBase = config.baseUrl, withPath = path)
    logger.info(s"In getJson before requestFor in [${Thread.currentThread().getName}]")
    requestFor(url, spanName, traceData).get().map {
      logger.info(s"In getJson after requestFor in [${Thread.currentThread().getName}]")
      fromResponseToErrorOrJson
    }(ec).recover(withTranslationOfFailureToError)(ec)
  }

  private def requestFor(url: String, spanName: String, traceData: TraceData): WSRequest =
    wsClient.
      url(url, spanName, traceData).
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
