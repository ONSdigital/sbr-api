package handlers

import services.ErrorMessage
import uk.gov.ons.sbr.models.LinkedUnit

trait LinkedUnitRetrievalHandler[A] {
  def apply(outcome: Either[ErrorMessage, Option[LinkedUnit]]): A
}
