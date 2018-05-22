package uk.gov.ons.sbr.models

import play.api.libs.json.{ JsResult, JsValue, Json, Reads }

case class UnitLinks(id: UnitId, unitType: UnitType, period: Period, children: Option[Map[UnitId, UnitType]])

object UnitLinks {
  val reads: Reads[UnitLinks] = ReadsUnitLinks

  /*
   * The default deserializer only supports Maps with String keys (given the underlying representation as a Json object).
   * To work around this, we simply model the keys as strings in an "external form", deserialize to that form, and then
   * convert the result.
   */
  private object ReadsUnitLinks extends Reads[UnitLinks] {
    private case class ExternalForm(id: UnitId, unitType: UnitType, period: Period, children: Option[Map[String, UnitType]])

    private implicit val readsUnitId: Reads[UnitId] = UnitId.JsonFormat
    private implicit val readsUnitType: Reads[UnitType] = UnitType.JsonFormat
    private implicit val readsPeriod: Reads[Period] = Period.JsonFormat
    private implicit val readsExternalForm: Reads[ExternalForm] = Json.reads[ExternalForm]

    override def reads(json: JsValue): JsResult[UnitLinks] =
      json.validate[ExternalForm].map(fromExternalForm)

    private def fromExternalForm(ef: ExternalForm): UnitLinks =
      UnitLinks(ef.id, ef.unitType, ef.period, ef.children.flatMap(fromExternalChildren))

    private def fromExternalChildren(children: Map[String, UnitType]): Option[Map[UnitId, UnitType]] = {
      val convertedChildren = convertChildKeyToUnitId(children)
      if (convertedChildren.isEmpty) None else Some(convertedChildren)
    }

    private def convertChildKeyToUnitId(children: Map[String, UnitType]): Map[UnitId, UnitType] =
      children.map {
        case (k, v) =>
          UnitId(k) -> v
      }
  }
}