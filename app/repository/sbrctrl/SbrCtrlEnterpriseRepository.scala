package repository.sbrctrl

import com.typesafe.scalalogging.LazyLogging
import javax.inject.Inject
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{ JsObject, JsValue }
import repository.EnterpriseRepository
import repository.sbrctrl.UnitRepository.ErrorMessage
import uk.gov.ons.sbr.models.{ Ern, Period }
import utils.TrySupport

import scala.concurrent.Future
import scala.util.Try

class SbrCtrlEnterpriseRepository @Inject() (unitRepository: UnitRepository) extends EnterpriseRepository with LazyLogging {
  override def retrieveEnterprise(period: Period, ern: Ern): Future[Either[ErrorMessage, Option[JsObject]]] = {
    val path = EnterprisePath(period, ern)
    logger.debug(s"Requesting enterprise with path [$path]")
    unitRepository.getJson(path).map { errorOrJson =>
      errorOrJson.right.flatMap {
        _.fold[Either[ErrorMessage, Option[JsObject]]](Right(None)) { jsValue =>
          asJsObject(jsValue).right.map(Some(_))
        }
      }
    }
  }

  private def asJsObject(jsValue: JsValue): Either[ErrorMessage, JsObject] =
    TrySupport.fold(Try(jsValue.as[JsObject]))(
      _ => Left(s"Json returned for Enterprise does not define an object [$jsValue]"),
      jsObject => Right(jsObject)
    )
}