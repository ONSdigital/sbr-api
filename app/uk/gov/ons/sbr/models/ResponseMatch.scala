package uk.gov.ons.sbr.models

import play.api.libs.json.{ JsValue, Json, OFormat }

/**
  * Created by haqa on 17/08/2017.
  */

sealed trait ResponseMatch

case class UnitMatch(
                      UnitLink: JsValue,
                      UnitRecord: JsValue
                    ) extends ResponseMatch

case class MultipleUnitsMatch(
                               UnitLink: JsValue
                             ) extends ResponseMatch


object ResponseMatch {

  implicit val unitFormat: OFormat[ResponseMatch] = Json.format[ResponseMatch]

  implicit class jsonConversion(val u: Seq[ResponseMatch]) {
    def toJson: JsValue = Json.toJson(u)
  }

}
