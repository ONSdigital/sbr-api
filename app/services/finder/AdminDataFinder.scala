package services.finder

import play.api.libs.json.JsObject
import repository.AdminDataRepository
import services.ErrorMessage
import tracing.TraceData
import uk.gov.ons.sbr.models.{Period, UnitLinks}
import unitref.UnitRef

import scala.concurrent.{ExecutionContext, Future}

class AdminDataFinder[T](unitRefType: UnitRef[T], adminDataRepository: AdminDataRepository)
                        (implicit ec: ExecutionContext) extends UnitFinder[T] {
  override def find(period: Period, unitRef: T, unitLinks: UnitLinks, traceData: TraceData): Future[Either[ErrorMessage, Option[JsObject]]] =
    adminDataRepository.retrieveAdminData(unitRefType.toUnitId(unitRef), period, traceData).map { errorOrAdminData =>
      errorOrAdminData.map { optAdminData =>
        optAdminData.map(_.variables)
      }
    }
}
