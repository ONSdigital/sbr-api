package services.admindata

import com.typesafe.scalalogging.LazyLogging
import javax.inject.{ Inject, Named }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repository.DataSourceNames.Vat
import repository.{ AdminDataRepository, UnitLinksRepository }
import services.{ ErrorMessage, VatService }
import uk.gov.ons.sbr.models.{ LinkedUnit, Period, UnitLinks, VatRef }

import scala.concurrent.Future

class AdminDataVatService @Inject() (unitLinksRepository: UnitLinksRepository, @Named(Vat) vatRepository: AdminDataRepository) extends VatService with LazyLogging {

  override def retrieve(period: Period, vatref: VatRef): Future[Either[ErrorMessage, Option[LinkedUnit]]] = {
    val (unitId, unitType) = VatRef.toIdTypePair(vatref)
    unitLinksRepository.retrieveUnitLinks(unitId, unitType, period).flatMap { errorOrUnitLinks =>
      errorOrUnitLinks.fold(
        err => Future.successful(Left(err)),
        optUnitLinks => optUnitLinks.fold(onUnitLinksNotFound(period, vatref)) { unitLinks =>
          onUnitLinksFound(period, vatref, unitLinks)
        }
      )
    }
  }

  private def onUnitLinksNotFound(period: Period, vatRef: VatRef): Future[Either[ErrorMessage, Option[LinkedUnit]]] = {
    logger.debug(s"No Unit Links found for [$period] and [$vatRef].")
    Future.successful(Right(None))
  }

  private def onUnitLinksFound(period: Period, vatRef: VatRef, unitLinks: UnitLinks): Future[Either[ErrorMessage, Option[LinkedUnit]]] = {
    logger.debug(s"Found Unit Links for [$period] and [$vatRef].  Requesting VAT admin data ...")
    vatRepository.retrieveAdminData(VatRef.asUnitId(vatRef), period).map { errorOrAdminData =>
      errorOrAdminData.right.map { optAdminData =>
        if (optAdminData.isEmpty) logger.warn(s"Inconsistent Database.  No VAT admin data found for [$period] and [$vatRef] that has Unit Links [$unitLinks].")
        optAdminData.map { adminData =>
          val linkedUnit = LinkedUnit.wrap(unitLinks, adminData.variables)
          logger.debug(s"LinkedUnit for [$period] and [$vatRef] is [$linkedUnit].")
          linkedUnit
        }
      }
    }
  }
}
