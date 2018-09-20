package repository

import uk.gov.ons.sbr.models.{ Period, VatRef }
import uk.gov.ons.sbr.models.edit.Patch

import scala.concurrent.Future

sealed trait EditParentLinkStatus
case object EditSuccess extends EditParentLinkStatus
case object EditRejected extends EditParentLinkStatus
case object EditUnitNotFound extends EditParentLinkStatus
case object EditFailure extends EditParentLinkStatus
case object EditConflict extends EditParentLinkStatus

trait AdminDataUnitLinksEditRepository {
  def patchVatParentUnitLink(patch: Patch, period: Period, vatref: VatRef): Future[EditParentLinkStatus]
}