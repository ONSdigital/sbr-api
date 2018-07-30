package repository

import play.api.libs.json.JsObject
import tracing.TraceData
import uk.gov.ons.sbr.models.{ Ern, Lurn, Period }

import scala.concurrent.Future

trait LocalUnitRepository {
  def retrieveLocalUnit(period: Period, ern: Ern, lurn: Lurn, traceData: TraceData): Future[Either[ErrorMessage, Option[JsObject]]]
}
