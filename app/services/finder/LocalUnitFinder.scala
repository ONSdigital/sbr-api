package services.finder

import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.JsObject
import repository.LocalUnitRepository
import services.ErrorMessage
import uk.gov.ons.sbr.models.UnitType.Enterprise
import uk.gov.ons.sbr.models.{ Ern, Lurn, Period, UnitLinks }
import unitref.UnitRef

import scala.concurrent.Future

/*
 * In order to lookup a Local Unit we need to extract the parent Enterprise reference from the unit links
 * (local units have a composite key based on period, ern & lurn).
 * The data model is broken if this parent does not exist - and so we log & error.
 */
class LocalUnitFinder(localUnitRepository: LocalUnitRepository, enterpriseUnitRefType: UnitRef[Ern]) extends UnitFinder[Lurn] with LazyLogging {
  override def find(period: Period, unitRef: Lurn, unitLinks: UnitLinks): Future[Either[ErrorMessage, Option[JsObject]]] =
    parentErnFrom(unitLinks).fold(Future.successful(onMissingParentEnterprise(unitRef))) { ern =>
      logger.debug(s"Attempting to retrieve local unit with [$ern] and [$unitRef] for [$period] ...")
      localUnitRepository.retrieveLocalUnit(period, ern, unitRef)
    }

  private def parentErnFrom(unitLinks: UnitLinks): Option[Ern] =
    unitLinks.parents.flatMap(_.get(Enterprise)).map {
      enterpriseUnitRefType.fromUnitId
    }

  private def onMissingParentEnterprise(lurn: Lurn): Either[ErrorMessage, Option[JsObject]] = {
    logger.warn(s"No parent Enterprise found in the unit links for local unit [$lurn].  Unable to retrieve the Local Unit!")
    Left(s"Unit Links for Local Unit [$lurn] is missing a parent Enterprise.")
  }
}
