package uk.gov.ons.sbr.models

import play.api.Logger
import play.api.libs.json._

import scala.util.{ Failure, Success, Try }

/**
 * Created by haqa on 17/08/2017.
 */

case class UnitMatch(
  UnitLink: JsValue,
  UnitRecord: JsValue
)

object UnitMatch {

  implicit val unitFormat: OFormat[UnitMatch] = Json.format[UnitMatch]

  //  implicit class jsonConversion(val u: Seq[UnitMatch]) {
  //    def toJson: JsValue = Json.toJson(u)
  //  }

  def toJson(record: JsValue, links: JsValue) = {
    // For BI, there is no "vars", just use the whole record
    val vars = Try((record \ "vars").get) match {
      case Success(s) => s
      case Failure(_) => record
    }
    // BI does not have period, so use an empty string
    val period = Try((record \ "period").get) match {
      case Success(s) => s
      case Failure(_) => Json.toJson("")
    }

    Json.obj(
      "id" -> (links \ "id").get,
      "parents" -> (links \ "parents").get,
      "children" -> (links \ "children").get,
      "unitType" -> (links \ "unitType").get,
      "period" -> period,
      "vars" -> vars
    )
  }

}
