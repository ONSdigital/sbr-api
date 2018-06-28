package repository.sbrctrl

import com.typesafe.scalalogging.LazyLogging
import javax.inject.{ Inject, Named }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Reads
import repository.DataSourceNames.SbrCtrl
import repository.rest.{ Repository, RepositoryResult }
import repository.{ ErrorMessage, UnitLinksRepository }
import uk.gov.ons.sbr.models.{ Period, UnitId, UnitLinks, UnitType }

import scala.concurrent.Future

class RestUnitLinksRepository @Inject() (@Named(SbrCtrl) unitRepository: Repository, readsUnitLinks: Reads[UnitLinks]) extends UnitLinksRepository with LazyLogging {
  override def retrieveUnitLinks(unitId: UnitId, unitType: UnitType, period: Period): Future[Either[ErrorMessage, Option[UnitLinks]]] = {
    val path = UnitLinksPath(unitId, unitType, period)
    logger.debug(s"Requesting Unit Links with path [$path].")
    unitRepository.getJson(path).map {
      RepositoryResult.as(readsUnitLinks)(_)
    }
  }
}
