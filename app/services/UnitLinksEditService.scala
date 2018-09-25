package services

import javax.inject.Inject

import com.typesafe.scalalogging.LazyLogging
import repository.AdminDataUnitLinksEditRepository
import repository.rest._
import uk.gov.ons.sbr.models._
import uk.gov.ons.sbr.models.Period
import uk.gov.ons.sbr.models.UnitType.LegalUnit

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class UnitLinksEditService @Inject() (repository: AdminDataUnitLinksEditRepository) extends EditService with LazyLogging {

  override def editVatParentUnitLink(period: Period, vatref: VatRef, editParentLink: EditParentLink): Future[EditParentLinkStatus] = {
    val parent = editParentLink.parent
    val unitKey = UnitKey(parent.to.id, LegalUnit, period)

    repository.updateVatParentUnitLink(parent.from, parent.to, vatref, period).flatMap {
      case PatchSuccess => Future.sequence(Seq(
        repository.createLeuChildUnitLink(unitKey, vatref)
      )).map(reducePatchStatuses)
      case failedStatus => Future(failedStatus)
    } map patchStatusToEditStatus
  }

  private def reducePatchStatuses(statuses: Seq[PatchStatus]): PatchStatus =
    if (statuses.exists(_ != PatchSuccess)) {
      logger.warn(s"One or more Patches submitted to sbr-control-api has failed with status: [$statuses]")
      PatchFailure
    } else PatchSuccess

  private def patchStatusToEditStatus(patch: PatchStatus): EditParentLinkStatus = patch match {
    case PatchSuccess => EditSuccess
    case PatchRejected => EditRejected
    case PatchUnitNotFound => EditUnitNotFound
    case PatchConflict => EditConflict
    case PatchFailure => EditFailure
  }
}