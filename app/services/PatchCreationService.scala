package services

import uk.gov.ons.sbr.models.EditParentLink
import uk.gov.ons.sbr.models.edit.Patch

trait PatchCreationService {
  def createPatch(edit: EditParentLink): Either[ErrorMessage, Patch]
}