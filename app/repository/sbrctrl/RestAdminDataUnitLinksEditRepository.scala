package repository.sbrctrl

import javax.inject.{ Inject, Named }

import com.typesafe.scalalogging.LazyLogging
import repository.DataSourceNames._
import repository._
import repository.rest._
import uk.gov.ons.sbr.models._
import uk.gov.ons.sbr.models.edit.Patch
import utils.edit.PatchCreation

import scala.concurrent._

class RestAdminDataUnitLinksEditRepository @Inject() (@Named(SbrCtrl) unitRepository: Repository) extends AdminDataUnitLinksEditRepository with LazyLogging {

  override def updateVatParentUnitLink(unitKey: UnitKey, from: IdAndType, to: IdAndType): Future[PatchStatus] = {
    val updatePatch = PatchCreation.buildUpdateParentPatch(from, to)
    createUrlAndPatch(unitKey, updatePatch)
  }

  override def createLeuChildUnitLink(unitKey: UnitKey, childToCreate: IdAndType): Future[PatchStatus] = {
    val createPatch = PatchCreation.buildCreateChildPatch(childToCreate)
    createUrlAndPatch(unitKey, createPatch)
  }

  override def deleteLeuChildUnitLink(unitKey: UnitKey, toDelete: IdAndType): Future[PatchStatus] = {
    val deletePatch = PatchCreation.buildDeleteLeuChildPatch(toDelete)
    createUrlAndPatch(unitKey, deletePatch)
  }

  private def createUrlAndPatch(unitKey: UnitKey, patch: Patch): Future[PatchStatus] = {
    val url = EditAdminDataPath(unitKey)
    logger.debug(s"Submitting patch [$patch] to sbr-control-api [$url]")
    unitRepository.patchJson(url, patch)
  }
}