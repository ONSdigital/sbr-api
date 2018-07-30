package services

import tracing.TraceData
import uk.gov.ons.sbr.models.{ LinkedUnit, Period }

import scala.concurrent.Future

trait LinkedUnitService[T] {
  def retrieve(period: Period, unitRef: T, traceData: TraceData): Future[Either[ErrorMessage, Option[LinkedUnit]]]
}
