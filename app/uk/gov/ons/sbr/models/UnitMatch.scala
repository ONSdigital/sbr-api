package uk.gov.ons.sbr.models

import play.api.libs.json.{JsValue, Json, OFormat}

/**
  * Created by haqa on 17/08/2017.
  */

case class UnitMatch(
                     UnitLink: String,
                     UnitRecord: String
                    )


object UnitMatch {

  implicit val unitFormat: OFormat[UnitMatch] = Json.format[UnitMatch]

  def toJson(u: UnitMatch): JsValue = Json.toJson(u)

}
