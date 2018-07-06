package repository

import play.api.libs.json.JsObject
import tracing.TraceData
import uk.gov.ons.sbr.models.{ Ern, Period, Rurn }

import scala.concurrent.Future

trait ReportingUnitRepository {
  def retrieveReportingUnit(period: Period, ern: Ern, rurn: Rurn, traceData: TraceData): Future[Either[ErrorMessage, Option[JsObject]]]
}
