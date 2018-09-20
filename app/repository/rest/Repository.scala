package repository.rest

import play.api.libs.json.JsValue
import repository.ErrorMessage
import tracing.TraceData
import uk.gov.ons.sbr.models.edit.Patch

import scala.concurrent.Future

sealed trait PatchStatus
case object PatchSuccess extends PatchStatus
case object PatchRejected extends PatchStatus
case object PatchUnitNotFound extends PatchStatus
case object PatchFailure extends PatchStatus
case object PatchConflict extends PatchStatus

trait Repository {
  def getJson(path: String, spanName: String, traceData: TraceData): Future[Either[ErrorMessage, Option[JsValue]]]
  def patchJson(path: String, patch: Patch): Future[PatchStatus]
}