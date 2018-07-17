package repository.sbrctrl

import com.typesafe.scalalogging.LazyLogging
import javax.inject.{ Inject, Named }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{ JsObject, Reads }
import repository.DataSourceNames.SbrCtrl
import repository.rest.{ Repository, RepositoryResult }
import repository.{ EnterpriseRepository, ErrorMessage }
import uk.gov.ons.sbr.models.{ Ern, Period }

import scala.concurrent.Future

class RestEnterpriseRepository @Inject() (@Named(SbrCtrl) unitRepository: Repository) extends EnterpriseRepository with LazyLogging {
  override def retrieveEnterprise(period: Period, ern: Ern): Future[Either[ErrorMessage, Option[JsObject]]] = {
    val path = EnterprisePath(period, ern)
    logger.debug(s"Requesting enterprise with path [$path]")
    unitRepository.getJson(path).map {
      RepositoryResult.as(Reads.JsObjectReads)(_)
    }
  }
}