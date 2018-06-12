package repository.admindata

import com.typesafe.scalalogging.LazyLogging
import javax.inject.Inject
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{ JsValue, Reads }
import repository.rest.UnitRepository
import repository.{ AdminDataRepository, ErrorMessage }
import uk.gov.ons.sbr.models.{ AdminData, Period, UnitId }

import scala.concurrent.Future

class RestAdminDataRepository @Inject() (unitRepository: UnitRepository, readsAdminData: Reads[AdminData]) extends AdminDataRepository with LazyLogging {
  override def retrieveAdminData(unitId: UnitId, period: Period): Future[Either[ErrorMessage, Option[AdminData]]] = {
    val path = AdminDataPath(unitId, period)
    logger.debug(s"Requesting Admin Data with path [$path].")
    unitRepository.getJson(path).map { errorOrJson =>
      errorOrJson.right.flatMap {
        _.fold[Either[ErrorMessage, Option[AdminData]]](Right(None)) { jsValue =>
          asAdminData(jsValue).right.map(Some(_))
        }
      }
    }
  }

  private def asAdminData(jsValue: JsValue): Either[ErrorMessage, AdminData] = {
    val errorsOrAdminData = jsValue.validate(readsAdminData).asEither
    errorsOrAdminData.left.map(errors => s"Unable to parse AdminData response [$errors]")
  }
}
