package services.finder

import play.api.libs.json.JsObject
import repository.EnterpriseRepository
import services.ErrorMessage
import tracing.TraceData
import uk.gov.ons.sbr.models.{ Ern, Period, UnitLinks }

import scala.concurrent.Future

class EnterpriseFinder(enterpriseRepository: EnterpriseRepository) extends UnitFinder[Ern] {
  override def find(period: Period, unitRef: Ern, unitLinks: UnitLinks, traceData: TraceData): Future[Either[ErrorMessage, Option[JsObject]]] =
    enterpriseRepository.retrieveEnterprise(period, unitRef, traceData)
}
