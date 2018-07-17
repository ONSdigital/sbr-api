package repository.sbrctrl

import com.typesafe.scalalogging.LazyLogging
import javax.inject.{ Inject, Named }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{ JsObject, Reads }
import repository.DataSourceNames.SbrCtrl
import repository.rest.{ Repository, RepositoryResult }
import repository.{ ErrorMessage, ReportingUnitRepository }
import uk.gov.ons.sbr.models.{ Ern, Period, Rurn }

import scala.concurrent.Future

class RestReportingUnitRepository @Inject() (@Named(SbrCtrl) unitRepository: Repository) extends ReportingUnitRepository with LazyLogging {
  override def retrieveReportingUnit(period: Period, ern: Ern, rurn: Rurn): Future[Either[ErrorMessage, Option[JsObject]]] = {
    val path = ReportingUnitPath(period, ern, rurn)
    logger.debug(s"Requesting reporting unit with path [$path]")
    unitRepository.getJson(path).map {
      RepositoryResult.as(Reads.JsObjectReads)(_)
    }
  }
}
