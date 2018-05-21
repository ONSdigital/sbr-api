package repository

import play.api.libs.json.JsObject
import uk.gov.ons.sbr.models.{ Ern, Period }

import scala.concurrent.Future

trait EnterpriseRepository {
  def retrieveEnterprise(period: Period, ern: Ern): Future[Either[ErrorMessage, Option[JsObject]]]
}
