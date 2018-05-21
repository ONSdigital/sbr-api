package repository

import uk.gov.ons.sbr.models.{ AdminData, Period, UnitId }

import scala.concurrent.Future

trait AdminDataRepository {
  def retrieveAdminData(unitId: UnitId, period: Period): Future[Either[ErrorMessage, Option[AdminData]]]
}
