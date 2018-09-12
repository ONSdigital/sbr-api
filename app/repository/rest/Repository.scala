package repository.rest

import play.api.libs.json.JsValue
import repository.{ EditParentLinkStatus, ErrorMessage }
import tracing.TraceData
import uk.gov.ons.sbr.models.edit.Patch

import scala.concurrent.Future

trait Repository {
  def getJson(path: String, spanName: String, traceData: TraceData): Future[Either[ErrorMessage, Option[JsValue]]]
  def patchJson(path: String, patch: Patch): Future[EditParentLinkStatus]
}