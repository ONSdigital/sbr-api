package repository.rest

import java.util.concurrent.TimeoutException

import com.typesafe.scalalogging.LazyLogging
import javax.inject.Inject
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.mvc.Http.HeaderNames.{ACCEPT, CONTENT_TYPE}
import play.mvc.Http.MimeTypes.JSON
import repository.ErrorMessage
import tracing.{TraceData, TraceWSClient}
import uk.gov.ons.sbr.models.edit.Patch
import utils.TrySupport
import utils.url.{BaseUrl, Url}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

case class RestRepositoryConfig(baseUrl: BaseUrl)

class RestRepository @Inject() (config: RestRepositoryConfig, wsClient: TraceWSClient)
                               (implicit ec: ExecutionContext) extends Repository with LazyLogging {

  // See here for details on JSON Patch: https://tools.ietf.org/html/rfc6902
  private val Patch_Json = s"$JSON-patch+json"

  override def getJson(path: String, spanName: String, traceData: TraceData): Future[Either[ErrorMessage, Option[JsValue]]] = {
    val url = Url(withBase = config.baseUrl, withPath = path)
    requestForWithTracing(url, JSON, spanName, traceData).get().map {
      fromResponseToErrorOrJson
    }.recover(withTranslationOfFailureToError)
  }

  override def patchJson(path: String, patch: Patch): Future[PatchStatus] = {
    val url = Url(withBase = config.baseUrl, withPath = path)
    requestFor(url, Patch_Json).patch(Json.toJson(patch)).map {
      toPatchStatus
    }.recover(withTranslationOfFailurePatchStatus)
  }

  private def requestForWithTracing(url: String, contentType: String, spanName: String, traceData: TraceData): WSRequest =
    wsClient.
      url(url, spanName, traceData).
      withHttpHeaders(ACCEPT -> contentType)

  /**
   * Explicit tracing will be removed in future, hence the duplication of the requestFor method below without
   * any explicit tracing parameters, using the untracedUrl method.
   */
  private def requestFor(url: String, contentType: String): WSRequest =
    wsClient.
      untracedUrl(url).
      withHttpHeaders(CONTENT_TYPE -> contentType)

  private def fromResponseToErrorOrJson(response: WSResponse): Either[ErrorMessage, Option[JsValue]] =
    response.status match {
      case OK => bodyAsJson(response).right.map(Some(_))
      case NOT_FOUND => Right(None)
      case _ => Left(describeStatus(response))
    }

  private def toPatchStatus(response: WSResponse): PatchStatus = {
    response.status match {
      case NO_CONTENT => PatchSuccess
      case NOT_FOUND => PatchUnitNotFound
      case CONFLICT => PatchConflict
      case UNPROCESSABLE_ENTITY => PatchRejected
      case _ => PatchFailure
    }
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

  private def withTranslationOfFailurePatchStatus = new PartialFunction[Throwable, PatchStatus] {
    override def isDefinedAt(cause: Throwable): Boolean = true

    override def apply(cause: Throwable): PatchStatus = {
      logger.error(s"Translating unit request failure [$cause].")
      PatchFailure
    }
  }
}
