package repository.sbrctrl

import javax.inject.{ Inject, Named }

import com.typesafe.scalalogging.LazyLogging
import repository.DataSourceNames._
import repository._
import repository.rest._
import uk.gov.ons.sbr.models._
import unitref.VatUnitRef
import utils.edit.PatchCreation

import scala.concurrent._

class RestAdminDataUnitLinksEditRepository @Inject() (@Named(SbrCtrl) unitRepository: Repository) extends AdminDataUnitLinksEditRepository with LazyLogging {

  override def updateVatParentUnitLink(from: IdAndType, to: IdAndType, vatref: VatRef, period: Period): Future[PatchStatus] = {
    val updatePatch = PatchCreation.buildUpdateParentPatch(from, to)
    val (unitId, unitType) = VatUnitRef.toIdTypePair(vatref)
    val url = EditAdminDataPath(UnitKey(unitId, unitType, period))
    logger.debug(s"Updating VAT parent unit link with url [$url] and patch [$updatePatch]")
    unitRepository.patchJson(url, updatePatch)
  }

  override def createLeuChildUnitLink(unitKey: UnitKey, vatref: VatRef): Future[PatchStatus] = {
    val (unitId, unitType) = VatUnitRef.toIdTypePair(vatref)
    val createPatch = PatchCreation.buildCreateChildPatch(unitId, unitType)
    val url = EditAdminDataPath(unitKey)
    logger.debug(s"Creating LEU child unit link with url [$url] and patch [$createPatch]")
    unitRepository.patchJson(url, createPatch)
  }
}