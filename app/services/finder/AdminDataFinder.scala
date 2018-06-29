package services.finder
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsObject
import repository.AdminDataRepository
import services.ErrorMessage
import uk.gov.ons.sbr.models.{ Period, UnitLinks }
import unitref.UnitRef

import scala.concurrent.Future

class AdminDataFinder[T](unitRefType: UnitRef[T], adminDataRepository: AdminDataRepository) extends UnitFinder[T] {
  override def find(period: Period, unitRef: T, unitLinks: UnitLinks): Future[Either[ErrorMessage, Option[JsObject]]] =
    adminDataRepository.retrieveAdminData(unitRefType.toUnitId(unitRef), period).map { errorOrAdminData =>
      errorOrAdminData.right.map { optAdminData =>
        optAdminData.map(_.variables)
      }
    }
}
