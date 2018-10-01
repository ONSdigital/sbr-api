package services

import javax.inject.Inject

import com.typesafe.scalalogging.LazyLogging
import repository.AdminDataUnitLinksEditRepository
import repository.rest._
import uk.gov.ons.sbr.models._
import uk.gov.ons.sbr.models.Period
import uk.gov.ons.sbr.models.UnitType.{ LegalUnit, ValueAddedTax }
import unitref.VatUnitRef

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class UnitLinksEditService @Inject() (repository: AdminDataUnitLinksEditRepository) extends EditService with LazyLogging {

  override def editVatParentUnitLink(period: Period, vatref: VatRef, editParentLink: EditParentLink): Future[EditParentLinkStatus] = {
    val parent = editParentLink.parent

    val updateParentUnitKey = UnitKey(UnitId(vatref.value), ValueAddedTax, period)
    val createChildUnitKey = UnitKey(parent.to.id, LegalUnit, period)
    val deleteChildUnitKey = UnitKey(parent.from.id, LegalUnit, period)

    val vatIdAndType = (IdAndType.apply _).tupled(VatUnitRef.toIdTypePair(vatref))

    repository.updateVatParentUnitLink(updateParentUnitKey, parent.from, parent.to).flatMap {
      case PatchSuccess => Future.sequence(Seq(
        repository.createLeuChildUnitLink(createChildUnitKey, vatIdAndType),
        repository.deleteLeuChildUnitLink(deleteChildUnitKey, vatIdAndType)
      )).map(reducePatchStatuses)
      case failedStatus => Future.successful(failedStatus)
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