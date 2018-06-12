package uk.gov.ons.sbr.models

import play.api.libs.json.{ JsObject, JsValue, Json, Writes }

/*
 * If you change this, see whether you need to change any of the Swagger "example" models under controllers.v1.api.
 * Ideally they would extend this class in order to prevent the Swagger examples from diverging from the real
 * implementation.  Unfortunately, the inability of Swagger to omit an optional Map field means that currently the
 * only way to get a good "example" is to use a "similar" class for the Swagger models that simply does not have the
 * fields you don't want to see.
 */
case class LinkedUnit(
  id: UnitId,
  unitType: UnitType,
  period: Period,
  parents: Option[Map[UnitType, UnitId]],
  children: Option[Map[UnitId, UnitType]],
  vars: JsObject
)

object LinkedUnit {
  val writes: Writes[LinkedUnit] = WritesLinkedUnit

  /*
   * The default serializer only supports Maps with String keys (given the underlying representation as a Json object).
   * To work around this, we simply model the keys as strings in an "external form", serialize to that form, and then
   * convert the result.
   */
  private object WritesLinkedUnit extends Writes[LinkedUnit] {
    private case class ExternalForm(
      id: UnitId,
      unitType: UnitType,
      period: Period,
      parents: Option[Map[String, UnitId]],
      children: Option[Map[String, UnitType]],
      vars: JsObject
    )

    private implicit val writesUnitId: Writes[UnitId] = UnitId.JsonFormat
    private implicit val writesUnitType: Writes[UnitType] = UnitType.JsonFormat
    private implicit val writesPeriod: Writes[Period] = Period.JsonFormat
    private implicit val writesExternalForm: Writes[ExternalForm] = Json.writes[ExternalForm]

    override def writes(lu: LinkedUnit): JsValue =
      writesExternalForm.writes(toExternalForm(lu))

    private def toExternalForm(lu: LinkedUnit): ExternalForm =
      ExternalForm(
        lu.id,
        lu.unitType,
        lu.period,
        lu.parents.map(toExternalParents),
        lu.children.map(toExternalChildren),
        lu.vars
      )

    private def toExternalParents(parents: Map[UnitType, UnitId]): Map[String, UnitId] =
      parents.map {
        case (unitType, id) =>
          UnitType.toAcronym(unitType) -> id
      }

    private def toExternalChildren(children: Map[UnitId, UnitType]): Map[String, UnitType] =
      children.map {
        case (id, unitType) =>
          id.value -> unitType
      }
  }

  def wrap(unitLinks: UnitLinks, unit: JsObject): LinkedUnit =
    LinkedUnit(unitLinks.id, unitLinks.unitType, unitLinks.period, unitLinks.parents, unitLinks.children, unit)
}