package services

import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.JsString
import uk.gov.ons.sbr.models.edit.OperationTypes.{ Replace, Test }
import uk.gov.ons.sbr.models.{ EditParentLink, IdAndType, UnitType }
import uk.gov.ons.sbr.models.edit.{ Operation, OperationType, Patch }

/**
 * Given the model our JsonBodyParser parses the incoming JSON into, we transform
 * that model into a multiple operations (a Patch). Business logic for creating the
 * path of the unit link to change will sit here.
 */
class UnitLinkPatchCreationService extends PatchCreationService with LazyLogging {

  override def createPatch(edit: EditParentLink): Either[ErrorMessage, Patch] = {
    // For now we can ignore audit information
    val parent = edit.parent
    Right(List(
      createOperation(parent.from, Test),
      createOperation(parent.to, Replace)
    ))
  }

  private def createOperation(idAndType: IdAndType, operationType: OperationType): Operation =
    Operation(operationType, formPatchPath(UnitType.toAcronym(idAndType.`type`)), JsString(idAndType.id.value))

  private def formPatchPath(unitType: String): String = s"/parents/$unitType"
}