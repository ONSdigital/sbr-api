package uk.gov.ons.sbr.models

import play.api.libs.json.{ JsObject, Json, Reads }

case class AdminData(id: UnitId, period: Period, variables: JsObject)

object AdminData {
  implicit val readsUnitId: Reads[UnitId] = UnitId.JsonFormat
  implicit val readsPeriod: Reads[Period] = Period.JsonFormat
  val reads = Json.reads[AdminData]
}