package repository

import play.api.libs.json.JsObject
import uk.gov.ons.sbr.models.{ Ern, Lurn, Period }

import scala.concurrent.Future

trait LocalUnitRepository {
  def retrieveLocalUnit(period: Period, ern: Ern, lurn: Lurn): Future[Either[ErrorMessage, Option[JsObject]]]
}
