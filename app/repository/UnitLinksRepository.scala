package repository

import repository.sbrctrl.UnitRepository.ErrorMessage
import uk.gov.ons.sbr.models._

import scala.concurrent.Future

trait UnitLinksRepository {
  def retrieveUnitLinks(unitId: UnitId, unitType: UnitType, period: Period): Future[Either[ErrorMessage, Option[UnitLinks]]]
}
