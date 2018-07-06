package repository

import tracing.TraceData
import uk.gov.ons.sbr.models.{ AdminData, Period, UnitId }

import scala.concurrent.Future

trait AdminDataRepository {
  def retrieveAdminData(unitId: UnitId, period: Period, traceData: TraceData): Future[Either[ErrorMessage, Option[AdminData]]]
}
