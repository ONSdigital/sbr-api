package utils.edit

import play.api.libs.json.JsString
import uk.gov.ons.sbr.models.{ IdAndType, UnitId, UnitType }
import uk.gov.ons.sbr.models.edit.OperationTypes.{ Add, Replace, Test }
import uk.gov.ons.sbr.models.edit.{ Operation, OperationType, Path, _ }

object PatchCreation {
  def buildUpdateParentPatch(from: IdAndType, to: IdAndType): Patch =
    Seq(createOperation(from, Test), createOperation(to, Replace))

  def buildCreateChildPatch(unitId: UnitId, unitType: UnitType): Patch =
    Seq(createChildOperation(unitId, Add, unitType))

  private def createOperation(idAndType: IdAndType, operationType: OperationType): Operation =
    Operation(operationType, createParentPatchPath(UnitType.toAcronym(idAndType.`type`)), JsString(idAndType.id.value))

  private def createParentPatchPath(unitType: String): Path = Path("/parents/", unitType)

  private def createChildPatchPath(childId: String): Path = Path("/children/", childId)

  /**
   * For operations on children, i.e. VAT/PAYE/CH records, the unitType (in the path) and the
   * id are swapped over. We use the unitType/id that corresponds with the endpoint that was hit, which
   * is passed through by the controller.
   */
  private def createChildOperation(id: UnitId, operationType: OperationType, unitType: UnitType): Operation =
    Operation(operationType, createChildPatchPath(id.value), JsString(UnitType.toAcronym(unitType)))
}