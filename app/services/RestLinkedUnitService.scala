package services

import com.typesafe.scalalogging.LazyLogging
import repository.UnitLinksRepository
import services.finder.UnitFinder
import tracing.TraceData
import uk.gov.ons.sbr.models.{LinkedUnit, Period, UnitLinks}
import unitref.UnitRef

import scala.concurrent.{ExecutionContext, Future}

class RestLinkedUnitService[T](
    unitRefType: UnitRef[T],
    unitLinksRepository: UnitLinksRepository,
    unitFinder: UnitFinder[T]
)(implicit ec: ExecutionContext) extends LinkedUnitService[T] with LazyLogging {

  override def retrieve(period: Period, unitRef: T, traceData: TraceData): Future[Either[ErrorMessage, Option[LinkedUnit]]] = {
    val (unitId, unitType) = unitRefType.toIdTypePair(unitRef)
    unitLinksRepository.retrieveUnitLinks(unitId, unitType, period, traceData).flatMap { errorOrUnitLinks =>
      errorOrUnitLinks.fold(
        err => Future.successful(Left(err)),
        optUnitLinks => optUnitLinks.fold(onUnitLinksNotFound(period, unitRef)) { unitLinks =>
          onUnitLinksFound(period, unitRef, unitLinks, traceData)
        }
      )
    }
  }

  private def onUnitLinksNotFound(period: Period, unitRef: T): Future[Either[ErrorMessage, Option[LinkedUnit]]] = {
    logger.debug(s"No Unit Links found for [$period] and [$unitRef].")
    Future.successful(Right(None))
  }

  private def onUnitLinksFound(period: Period, unitRef: T, unitLinks: UnitLinks, traceData: TraceData): Future[Either[ErrorMessage, Option[LinkedUnit]]] = {
    logger.debug(s"Found Unit Links for [$period] and [$unitRef].  Attempting to find unit ...")
    unitFinder.find(period, unitRef, unitLinks, traceData).map { errorOrJson =>
      errorOrJson.right.map { optJson =>
        if (optJson.isEmpty) logger.warn(s"Inconsistent Database.  No unit found for [$period] and [$unitRef] that has Unit Links [$unitLinks].")
        optJson.map { json =>
          val linkedUnit = LinkedUnit.wrap(unitLinks, json)
          logger.debug(s"LinkedUnit for [$period] and [$unitRef] is [$linkedUnit].")
          linkedUnit
        }
      }
    }
  }
}
