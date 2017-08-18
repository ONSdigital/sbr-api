package uk.gov.ons.sbr.models

import play.api.libs.json.{ JsValue, Json, OFormat }

/**
 * Created by haqa on 17/08/2017.
 */

case class UnitMatch(
  UnitLink: JsValue,
  UnitRecord: JsValue
)

object UnitMatch {

  implicit val unitFormat: OFormat[UnitMatch] = Json.format[UnitMatch]

  implicit class jsonConversion(val u: Seq[UnitMatch]) {
    def toJson: JsValue = Json.toJson(u)
  }

}
