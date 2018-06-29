package repository.sbrctrl

import com.typesafe.scalalogging.LazyLogging
import javax.inject.{ Inject, Named }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{ JsObject, JsValue }
import repository.DataSourceNames.SbrCtrl
import repository.rest.UnitRepository
import repository.{ ErrorMessage, LocalUnitRepository }
import uk.gov.ons.sbr.models.{ Ern, Lurn, Period }
import utils.TrySupport

import scala.concurrent.Future
import scala.util.Try

class RestLocalUnitRepository @Inject() (@Named(SbrCtrl) unitRepository: UnitRepository) extends LocalUnitRepository with LazyLogging {
  override def retrieveLocalUnit(period: Period, ern: Ern, lurn: Lurn): Future[Either[ErrorMessage, Option[JsObject]]] = {
    val path = LocalUnitPath(period, ern, lurn)
    logger.debug(s"Requesting local unit with path [$path]")
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
      _ => Left(s"Json returned for Local Unit does not define an object [$jsValue]"),
      jsObject => Right(jsObject)
    )
}
