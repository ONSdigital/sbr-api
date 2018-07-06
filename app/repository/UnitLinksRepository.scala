package repository

import tracing.TraceData
import uk.gov.ons.sbr.models._

import scala.concurrent.Future

trait UnitLinksRepository {
  def retrieveUnitLinks(unitId: UnitId, unitType: UnitType, period: Period, traceData: TraceData): Future[Either[ErrorMessage, Option[UnitLinks]]]
}
