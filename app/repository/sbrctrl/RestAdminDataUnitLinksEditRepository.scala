package repository.sbrctrl

import javax.inject.{ Inject, Named }

import com.typesafe.scalalogging.LazyLogging
import repository.DataSourceNames._
import repository._
import repository.rest._
import uk.gov.ons.sbr.models.{ Period, VatRef }
import uk.gov.ons.sbr.models.edit.Patch
import unitref.VatUnitRef

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

class RestAdminDataUnitLinksEditRepository @Inject() (@Named(SbrCtrl) unitRepository: Repository) extends AdminDataUnitLinksEditRepository with LazyLogging {

  override def patchVatParentUnitLink(patch: Patch, period: Period, vatref: VatRef): Future[EditParentLinkStatus] = {
    val path = EditAdminDataPath(period, VatUnitRef.toIdTypePair(vatref))
    unitRepository.patchJson(path, patch).map(patchStatusToEditStatus)
  }

  private def patchStatusToEditStatus(patch: PatchStatus): EditParentLinkStatus = patch match {
    case PatchSuccess => EditSuccess
    case PatchRejected => EditRejected
    case PatchUnitNotFound => EditUnitNotFound
    case PatchConflict => EditConflict
    case PatchFailure => EditFailure
  }
}