package services

import javax.inject.Inject

import com.typesafe.scalalogging.LazyLogging
import repository.AdminDataUnitLinksEditRepository
import repository.rest._
import uk.gov.ons.sbr.models._
import uk.gov.ons.sbr.models.Period
import unitref.{ PayeUnitRef, VatUnitRef }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class UnitLinksEditService @Inject() (repository: AdminDataUnitLinksEditRepository) extends EditService with LazyLogging {

  override def editVatAdminDataParentUnitLink(period: Period, vatref: VatRef, editParentLink: EditParentLink): Future[EditParentLinkStatus] = {
    val idAndType = (IdAndType.apply _).tupled(VatUnitRef.toIdTypePair(vatref))
    submitEditRequests(period, idAndType, editParentLink.parent)
  }

  override def editPayeAdminDataParentUnitLink(period: Period, payeref: PayeRef, editParentLink: EditParentLink): Future[EditParentLinkStatus] = {
    val idAndType = (IdAndType.apply _).tupled(PayeUnitRef.toIdTypePair(payeref))
    submitEditRequests(period, idAndType, editParentLink.parent)
  }

  private def submitEditRequests(period: Period, idAndType: IdAndType, parent: Parent): Future[EditParentLinkStatus] = {
    val updateParentUnitKey = UnitKey(idAndType.id, idAndType.`type`, period)
    val createChildUnitKey = UnitKey(parent.to.id, parent.to.`type`, period)
    val deleteChildUnitKey = UnitKey(parent.from.id, parent.from.`type`, period)

    updateAdminDataParentUnitLinkAndHandleConflict(updateParentUnitKey, parent) flatMap {
      case PatchSuccess => Future.sequence(Seq(
        repository.createLeuChildUnitLink(createChildUnitKey, idAndType),
        repository.deleteLeuChildUnitLink(deleteChildUnitKey, idAndType)
      )).map(reducePatchStatuses)
      case failedStatus => Future.successful(failedStatus)
    } map patchStatusToEditStatus
  }

  /**
   * When the user submits an editAdminDataParentUnitLink request, if the update operation succeeds but any of the
   * subsequent create/delete operations fail, the request will need to be retried by the user, to maintain
   * data consistency in HBase.
   *
   * If the user re-submits the request (with the same JSON), as the first update operation succeeded before, that
   * same operation now returns a conflict as the from value has been updated. In this scenario, we submit the same
   * request, just with the from/to value being the updated (to) value, thus the request will succeed and the
   * subsequent add/delete operations can be submitted.
   */
  private def updateAdminDataParentUnitLinkAndHandleConflict(unitKey: UnitKey, parent: Parent): Future[PatchStatus] = {
    repository.updateAdminDataParentUnitLink(unitKey, parent.from, parent.to) flatMap {
      case PatchConflict => repository.updateAdminDataParentUnitLink(unitKey, parent.to, parent.to)
      case patchStatus => Future.successful(patchStatus)
    }
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