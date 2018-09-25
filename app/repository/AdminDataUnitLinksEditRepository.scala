package repository

import repository.rest.PatchStatus
import uk.gov.ons.sbr.models._

import scala.concurrent.Future

trait AdminDataUnitLinksEditRepository {
  def updateVatParentUnitLink(from: IdAndType, to: IdAndType, vatref: VatRef, period: Period): Future[PatchStatus]
  def createLeuChildUnitLink(unitKey: UnitKey, vatRef: VatRef): Future[PatchStatus]
}