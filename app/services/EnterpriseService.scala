package services

import uk.gov.ons.sbr.models.{ Ern, LinkedUnit, Period }

import scala.concurrent.Future

trait EnterpriseService {
  def retrieve(period: Period, ern: Ern): Future[Either[ErrorMessage, Option[LinkedUnit]]]
}
