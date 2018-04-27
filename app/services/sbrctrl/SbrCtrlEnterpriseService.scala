package services.sbrctrl

import com.typesafe.scalalogging.LazyLogging
import javax.inject.Inject
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repository.{ EnterpriseRepository, UnitLinksRepository }
import services.EnterpriseService
import services.EnterpriseService.ErrorMessage
import uk.gov.ons.sbr.models._

import scala.concurrent.Future

class SbrCtrlEnterpriseService @Inject() (unitLinksRepository: UnitLinksRepository, enterpriseRepository: EnterpriseRepository) extends EnterpriseService with LazyLogging {

  override def retrieve(period: Period, ern: Ern): Future[Either[ErrorMessage, Option[LinkedUnit]]] = {
    val (unitId, unitType) = Ern.toIdTypePair(ern)
    unitLinksRepository.retrieveUnitLinks(unitId, unitType, period).flatMap { errorOrUnitLinks =>
      errorOrUnitLinks.fold(
        err => Future.successful(Left(err)),
        optUnitLinks => optUnitLinks.fold(onUnitLinksNotFound(period, ern)) { unitLinks =>
          onUnitLinksFound(period, ern, unitLinks)
        }
      )
    }
  }

  private def onUnitLinksNotFound(period: Period, ern: Ern): Future[Either[ErrorMessage, Option[LinkedUnit]]] = {
    logger.debug(s"No Unit Links found for [$period] and [$ern].")
    Future.successful(Right(None))
  }

  private def onUnitLinksFound(period: Period, ern: Ern, unitLinks: UnitLinks): Future[Either[ErrorMessage, Option[LinkedUnit]]] = {
    logger.debug(s"Found Unit Links for [$period] and [$ern].  Requesting enterprise ...")
    enterpriseRepository.retrieveEnterprise(period, ern).map { errorOrJson =>
      errorOrJson.right.map { optJson =>
        if (optJson.isEmpty) logger.warn(s"Inconsistent Database.  No enterprise found for [$period] and [$ern] that has Unit Links [$unitLinks].")
        optJson.map { json =>
          val linkedUnit = LinkedUnit.wrap(unitLinks, json)
          logger.debug(s"LinkedUnit for [$period] and [$ern] is [$linkedUnit].")
          linkedUnit
        }
      }
    }
  }
}
