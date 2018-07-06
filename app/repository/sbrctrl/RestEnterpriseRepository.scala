package repository.sbrctrl

import com.typesafe.scalalogging.LazyLogging
import javax.inject.{ Inject, Named }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{ JsObject, Reads }
import repository.DataSourceNames.SbrCtrl
import repository.rest.{ Repository, RepositoryResult }
import repository.{ EnterpriseRepository, ErrorMessage }
import tracing.TraceData
import uk.gov.ons.sbr.models.{ Ern, Period }

import scala.concurrent.Future

class RestEnterpriseRepository @Inject() (@Named(SbrCtrl) unitRepository: Repository) extends EnterpriseRepository with LazyLogging {
  override def retrieveEnterprise(period: Period, ern: Ern, traceData: TraceData): Future[Either[ErrorMessage, Option[JsObject]]] = {
    val path = EnterprisePath(period, ern)
    logger.debug(s"Requesting enterprise with path [$path]")
    unitRepository.getJson(path, spanName = "get-enterprise", traceData).map {
      RepositoryResult.as(Reads.JsObjectReads)(_)
    }
  }
}