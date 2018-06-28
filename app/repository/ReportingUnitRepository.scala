package repository

import play.api.libs.json.JsObject
import uk.gov.ons.sbr.models.{ Ern, Period, Rurn }

import scala.concurrent.Future

trait ReportingUnitRepository {
  def retrieveReportingUnit(period: Period, ern: Ern, rurn: Rurn): Future[Either[ErrorMessage, Option[JsObject]]]
}
