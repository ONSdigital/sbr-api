package utils.edit

import play.api.libs.json.JsString
import uk.gov.ons.sbr.models.edit._
import uk.gov.ons.sbr.models.{IdAndType, UnitType}

object PatchCreation {

  def buildUpdateParentPatch(from: IdAndType, to: IdAndType): Patch = Seq(
    TestOperation(createParentPath(UnitType.toAcronym(from.`type`)), JsString(from.id.value)),
    ReplaceOperation(createParentPath(UnitType.toAcronym(to.`type`)), JsString(to.id.value))
  )

  def buildCreateChildPatch(idAndType: IdAndType): Patch = Seq(
    AddOperation(createChildPath(idAndType.id.value), JsString(UnitType.toAcronym(idAndType.`type`)))
  )

  def buildDeleteLeuChildPatch(idAndType: IdAndType): Patch = Seq(
    TestOperation(createChildPath(idAndType.id.value), JsString(UnitType.toAcronym(idAndType.`type`))),
    RemoveOperation(createChildPath(idAndType.id.value))
  )

  private def createChildPath(value: String): Path = Path("/children/", value)

  private def createParentPath(value: String): Path = Path("/parents/", value)
}