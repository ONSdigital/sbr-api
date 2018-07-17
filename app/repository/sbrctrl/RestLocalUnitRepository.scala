package repository.sbrctrl

import com.typesafe.scalalogging.LazyLogging
import javax.inject.{ Inject, Named }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{ JsObject, Reads }
import repository.DataSourceNames.SbrCtrl
import repository.rest.{ Repository, RepositoryResult }
import repository.{ ErrorMessage, LocalUnitRepository }
import uk.gov.ons.sbr.models.{ Ern, Lurn, Period }

import scala.concurrent.Future

class RestLocalUnitRepository @Inject() (@Named(SbrCtrl) unitRepository: Repository) extends LocalUnitRepository with LazyLogging {
  override def retrieveLocalUnit(period: Period, ern: Ern, lurn: Lurn): Future[Either[ErrorMessage, Option[JsObject]]] = {
    val path = LocalUnitPath(period, ern, lurn)
    logger.debug(s"Requesting local unit with path [$path]")
    unitRepository.getJson(path).map {
      RepositoryResult.as(Reads.JsObjectReads)(_)
    }
  }
}
