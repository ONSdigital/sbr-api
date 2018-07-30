package repository.sbrctrl

import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Reads
import repository.rest.{ Repository, RepositoryResult }
import repository.{ ErrorMessage, UnitLinksRepository }
import tracing.TraceData
import uk.gov.ons.sbr.models.{ Period, UnitId, UnitLinks, UnitType }

import scala.concurrent.{ ExecutionContext, Future }

class RestUnitLinksRepository(unitRepository: Repository, readsUnitLinks: Reads[UnitLinks], ec: ExecutionContext) extends UnitLinksRepository with LazyLogging {
  override def retrieveUnitLinks(unitId: UnitId, unitType: UnitType, period: Period, traceData: TraceData): Future[Either[ErrorMessage, Option[UnitLinks]]] = {
    val path = UnitLinksPath(unitId, unitType, period)
    logger.debug(s"Requesting Unit Links with path [$path].")
    unitRepository.getJson(path, spanName = "get-unit-links", traceData).map {
      RepositoryResult.as(readsUnitLinks)(_)
    }(ec)
  }
}
