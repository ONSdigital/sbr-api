package services.finder
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.JsObject
import services.ErrorMessage
import services.finder.ByParentEnterpriseUnitFinder.UnitRetrieval
import uk.gov.ons.sbr.models.{ Ern, Period, UnitLinks }
import unitref.UnitRef

import scala.concurrent.Future

class ByParentEnterpriseUnitFinder[T](retrieveUnit: UnitRetrieval[T], enterpriseUnitRefType: UnitRef[Ern]) extends UnitFinder[T] with LazyLogging {
  override def find(period: Period, unitRef: T, unitLinks: UnitLinks): Future[Either[ErrorMessage, Option[JsObject]]] =
    UnitLinks.parentErnFrom(enterpriseUnitRefType)(unitLinks).fold(Future.successful(onMissingParentEnterprise(unitRef))) { ern =>
      logger.debug(s"Attempting to retrieve unit with [$ern] and [$unitRef] for [$period] ...")
      retrieveUnit(period, ern, unitRef)
    }

  private def onMissingParentEnterprise(unitRef: T): Either[ErrorMessage, Option[JsObject]] = {
    logger.warn(s"Invalid data.  The unit [$unitRef] has Unit Links that do not contain the mandatory parent Enterprise.")
    Left(s"Unit Links for unit [$unitRef] is missing the mandatory parent Enterprise.")
  }
}

object ByParentEnterpriseUnitFinder {
  type UnitRetrieval[T] = (Period, Ern, T) => Future[Either[ErrorMessage, Option[JsObject]]]
}