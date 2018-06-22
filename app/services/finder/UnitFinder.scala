package services.finder

import play.api.libs.json.JsObject
import services.ErrorMessage
import uk.gov.ons.sbr.models.{ Period, UnitLinks }

import scala.concurrent.Future

trait UnitFinder[T] {
  def find(period: Period, unitRef: T, unitLinks: UnitLinks): Future[Either[ErrorMessage, Option[JsObject]]]
}
