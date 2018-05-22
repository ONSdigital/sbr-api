package uk.gov.ons.sbr.models

import io.swagger.annotations.ApiModelProperty
import play.api.libs.json.{ JsObject, JsValue, Json, Writes }

/*
* DO NOT USE.
* This only exists to help Swagger generate a correct example of a LinkedUnit with children.
*/
@deprecated(message = "This is just for Swagger example purposes - do not use in real code", since = "")
private[models] case class ExampleChildrenForSwagger(
  @ApiModelProperty(value = "the unique identifier of the child followed by the child's type", dataType = "string", example = "LEU", required = true) `10205415`: UnitType,
  @ApiModelProperty(value = "the unique identifier of the child followed by the child's type", dataType = "string", example = "LOU", required = true) `900000011`: UnitType
)

case class LinkedUnit(
  @ApiModelProperty(value = "the unique identifier of the unit (such as the ERN or LURN)", dataType = "string", example = "1234567890", required = true) id: UnitId,
  @ApiModelProperty(value = "the type of unit such as ENT or LOU", dataType = "string", example = "ENT", required = true) unitType: UnitType,
  @ApiModelProperty(value = "Period (unit load date) - in YYYYMM format", dataType = "string", example = "201803", required = true) period: Period,
  @ApiModelProperty(value = "the identifiers of child units along with the associated unit type", dataType = "uk.gov.ons.sbr.models.ExampleChildrenForSwagger", required = false) children: Option[Map[UnitId, UnitType]],
  @ApiModelProperty(value = "the representation of the unit itself", dataType = "object", required = true) vars: JsObject
)

object LinkedUnit {
  val writes: Writes[LinkedUnit] = WritesLinkedUnit

  /*
   * The default serializer only supports Maps with String keys (given the underlying representation as a Json object).
   * To work around this, we simply model the keys as strings in an "external form", serialize to that form, and then
   * convert the result.
   */
  private object WritesLinkedUnit extends Writes[LinkedUnit] {
    private case class ExternalForm(id: UnitId, unitType: UnitType, period: Period, children: Option[Map[String, UnitType]], vars: JsObject)

    private implicit val writesUnitId: Writes[UnitId] = UnitId.JsonFormat
    private implicit val writesUnitType: Writes[UnitType] = UnitType.JsonFormat
    private implicit val writesPeriod: Writes[Period] = Period.JsonFormat
    private implicit val writesExternalForm: Writes[ExternalForm] = Json.writes[ExternalForm]

    override def writes(lu: LinkedUnit): JsValue =
      writesExternalForm.writes(toExternalForm(lu))

    private def toExternalForm(lu: LinkedUnit): ExternalForm =
      ExternalForm(lu.id, lu.unitType, lu.period, lu.children.map(toExternalChildren), lu.vars)

    private def toExternalChildren(children: Map[UnitId, UnitType]): Map[String, UnitType] =
      children.map {
        case (id, unitType) =>
          id.value -> unitType
      }
  }

  def wrap(unitLinks: UnitLinks, unit: JsObject): LinkedUnit =
    LinkedUnit(unitLinks.id, unitLinks.unitType, unitLinks.period, unitLinks.children, unit)
}