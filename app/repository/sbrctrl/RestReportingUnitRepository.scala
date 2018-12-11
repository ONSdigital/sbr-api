package repository.sbrctrl

import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Named}
import play.api.libs.json.{JsObject, Reads}
import repository.DataSourceNames.SbrCtrl
import repository.rest.{Repository, RepositoryResult}
import repository.{ErrorMessage, ReportingUnitRepository}
import tracing.TraceData
import uk.gov.ons.sbr.models.{Ern, Period, Rurn}

import scala.concurrent.{ExecutionContext, Future}

class RestReportingUnitRepository @Inject() (@Named(SbrCtrl) unitRepository: Repository)
                                            (implicit ec: ExecutionContext) extends ReportingUnitRepository with LazyLogging {
  override def retrieveReportingUnit(period: Period, ern: Ern, rurn: Rurn, traceData: TraceData): Future[Either[ErrorMessage, Option[JsObject]]] = {
    val path = ReportingUnitPath(period, ern, rurn)
    logger.debug(s"Requesting reporting unit with path [$path]")
    unitRepository.getJson(path, spanName = "get-reporting-unit", traceData).map {
      RepositoryResult.as(Reads.JsObjectReads)(_)
    }
  }
}
