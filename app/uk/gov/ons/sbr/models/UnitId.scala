package uk.gov.ons.sbr.models

import play.api.libs.json.Reads.JsStringReads
import play.api.libs.json.{ Format, JsResult, JsString, JsValue }
case class UnitId(value: String)

object UnitId {
  /*
   * Reads from / writes to a simple Json string.
   */
  object JsonFormat extends Format[UnitId] {
    override def reads(json: JsValue): JsResult[UnitId] =
      JsStringReads.reads(json).map { jsonStr =>
        UnitId(jsonStr.value)
      }

    override def writes(unitId: UnitId): JsValue =
      JsString(unitId.value)
  }
}