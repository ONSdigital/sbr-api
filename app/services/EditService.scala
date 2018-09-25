package services

import uk.gov.ons.sbr.models.{ EditParentLink, Period, VatRef }

import scala.concurrent.Future

sealed trait EditParentLinkStatus
case object EditSuccess extends EditParentLinkStatus
case object EditRejected extends EditParentLinkStatus
case object EditUnitNotFound extends EditParentLinkStatus
case object EditFailure extends EditParentLinkStatus
case object EditConflict extends EditParentLinkStatus

trait EditService {
  def editVatParentUnitLink(period: Period, vatref: VatRef, editParentLink: EditParentLink): Future[EditParentLinkStatus]
}