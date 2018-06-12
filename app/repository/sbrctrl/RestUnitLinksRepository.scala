package repository.sbrctrl

import com.typesafe.scalalogging.LazyLogging
import javax.inject.{ Inject, Named }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{ JsValue, Reads }
import repository.DataSourceNames.SbrCtrl
import repository.rest.UnitRepository
import repository.{ ErrorMessage, UnitLinksRepository }
import uk.gov.ons.sbr.models.{ Period, UnitId, UnitLinks, UnitType }

import scala.concurrent.Future

class RestUnitLinksRepository @Inject() (@Named(SbrCtrl) unitRepository: UnitRepository, readsUnitLinks: Reads[UnitLinks]) extends UnitLinksRepository with LazyLogging {

  override def retrieveUnitLinks(unitId: UnitId, unitType: UnitType, period: Period): Future[Either[ErrorMessage, Option[UnitLinks]]] = {
    val path = UnitLinksPath(unitId, unitType, period)
    logger.debug(s"Requesting Unit Links with path [$path].")
    unitRepository.getJson(path).map { errorOrJson =>
      errorOrJson.right.flatMap {
        _.fold[Either[ErrorMessage, Option[UnitLinks]]](Right(None)) { jsValue =>
          asUnitLinks(jsValue).right.map(Some(_))
        }
      }
    }
  }

  private def asUnitLinks(jsValue: JsValue): Either[ErrorMessage, UnitLinks] = {
    val errorsOrUnitLinks = jsValue.validate(readsUnitLinks).asEither
    errorsOrUnitLinks.left.map(errors => s"Unable to parse UnitLinks response [$errors]")
  }
}
