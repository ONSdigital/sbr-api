package repository

import repository.rest.PatchStatus
import uk.gov.ons.sbr.models._

import scala.concurrent.Future

/**
 * Each unitKey is used to form the sbr-control-api URL and other parameters are used to form the Patch which is
 * sent to sbr-control-api.
 */
trait AdminDataUnitLinksEditRepository {
  def updateAdminDataParentUnitLink(unitKey: UnitKey, from: IdAndType, to: IdAndType): Future[PatchStatus]
  def createLeuChildUnitLink(unitKey: UnitKey, childToCreate: IdAndType): Future[PatchStatus]
  def deleteLeuChildUnitLink(unitKey: UnitKey, toDelete: IdAndType): Future[PatchStatus]
}