package services

import uk.gov.ons.sbr.models.{ LinkedUnit, Period, VatRef }

import scala.concurrent.Future

trait VatService {
  def retrieve(period: Period, vatref: VatRef): Future[Either[ErrorMessage, Option[LinkedUnit]]]
}
