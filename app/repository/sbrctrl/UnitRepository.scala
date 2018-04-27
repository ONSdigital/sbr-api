package repository.sbrctrl

import play.api.libs.json.JsValue
import repository.sbrctrl.UnitRepository.ErrorMessage

import scala.concurrent.Future

trait UnitRepository {
  def getJson(path: String): Future[Either[ErrorMessage, Option[JsValue]]]
}

object UnitRepository {
  type ErrorMessage = String
}