package services.admindata

import com.typesafe.scalalogging.LazyLogging
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repository.{ AdminDataRepository, UnitLinksRepository }
import services.{ ErrorMessage, LinkedUnitService }
import uk.gov.ons.sbr.models.{ LinkedUnit, Period, UnitLinks }
import unitref.UnitRef

import scala.concurrent.Future

class AdminDataService[T](
    unitRefType: UnitRef[T],
    unitLinksRepository: UnitLinksRepository,
    adminDataRepository: AdminDataRepository
) extends LinkedUnitService[T] with LazyLogging {

  override def retrieve(period: Period, unitRef: T): Future[Either[ErrorMessage, Option[LinkedUnit]]] = {
    val (unitId, unitType) = unitRefType.toIdTypePair(unitRef)
    unitLinksRepository.retrieveUnitLinks(unitId, unitType, period).flatMap { errorOrUnitLinks =>
      errorOrUnitLinks.fold(
        err => Future.successful(Left(err)),
        optUnitLinks => optUnitLinks.fold(onUnitLinksNotFound(period, unitRef)) { unitLinks =>
          onUnitLinksFound(period, unitRef, unitLinks)
        }
      )
    }
  }

  private def onUnitLinksNotFound(period: Period, unitRef: T): Future[Either[ErrorMessage, Option[LinkedUnit]]] = {
    logger.debug(s"No Unit Links found for [$period] and [$unitRef].")
    Future.successful(Right(None))
  }

  private def onUnitLinksFound(period: Period, unitRef: T, unitLinks: UnitLinks): Future[Either[ErrorMessage, Option[LinkedUnit]]] = {
    logger.debug(s"Found Unit Links for [$period] and [$unitRef].  Requesting admin data ...")
    adminDataRepository.retrieveAdminData(unitRefType.toUnitId(unitRef), period).map { errorOrAdminData =>
      errorOrAdminData.right.map { optAdminData =>
        if (optAdminData.isEmpty) logger.warn(s"Inconsistent Database.  No admin data found for [$period] and [$unitRef] that has Unit Links [$unitLinks].")
        optAdminData.map { adminData =>
          val linkedUnit = LinkedUnit.wrap(unitLinks, adminData.variables)
          logger.debug(s"LinkedUnit for [$period] and [$unitRef] is [$linkedUnit].")
          linkedUnit
        }
      }
    }
  }
}
