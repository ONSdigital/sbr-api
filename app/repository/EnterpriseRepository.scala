package repository

import play.api.libs.json.JsObject
import tracing.TraceData
import uk.gov.ons.sbr.models.{ Ern, Period }

import scala.concurrent.Future

trait EnterpriseRepository {
  def retrieveEnterprise(period: Period, ern: Ern, traceData: TraceData): Future[Either[ErrorMessage, Option[JsObject]]]
}
