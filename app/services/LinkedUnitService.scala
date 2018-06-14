package services

import uk.gov.ons.sbr.models.{ LinkedUnit, Period }

import scala.concurrent.Future

trait LinkedUnitService[T] {
  def retrieve(period: Period, unitRef: T): Future[Either[ErrorMessage, Option[LinkedUnit]]]
}
