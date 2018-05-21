package repository.rest

import play.api.libs.json.JsValue
import repository.ErrorMessage

import scala.concurrent.Future

trait UnitRepository {
  def getJson(path: String): Future[Either[ErrorMessage, Option[JsValue]]]
}