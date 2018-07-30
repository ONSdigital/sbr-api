package repository.rest

import play.api.libs.json.JsValue
import repository.ErrorMessage
import tracing.TraceData

import scala.concurrent.Future

trait Repository {
  def getJson(path: String, spanName: String, traceData: TraceData): Future[Either[ErrorMessage, Option[JsValue]]]
}