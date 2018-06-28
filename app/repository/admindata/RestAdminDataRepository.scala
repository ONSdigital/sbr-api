package repository.admindata

import com.typesafe.scalalogging.LazyLogging
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Reads
import repository.rest.{ Repository, RepositoryResult }
import repository.{ AdminDataRepository, ErrorMessage }
import uk.gov.ons.sbr.models.{ AdminData, Period, UnitId }

import scala.concurrent.Future

class RestAdminDataRepository(unitRepository: Repository, readsAdminData: Reads[AdminData]) extends AdminDataRepository with LazyLogging {
  override def retrieveAdminData(unitId: UnitId, period: Period): Future[Either[ErrorMessage, Option[AdminData]]] = {
    val path = AdminDataPath(unitId, period)
    logger.debug(s"Requesting Admin Data with path [$path].")
    unitRepository.getJson(path).map {
      RepositoryResult.as(readsAdminData)(_)
    }
  }
}
