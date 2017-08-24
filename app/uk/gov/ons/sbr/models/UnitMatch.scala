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

  def toJson(record: Seq[JsValue], links: Seq[JsValue]): JsValue = {
    val a = (links zip record).map(
      z => {
        // For BI, there is no "vars", just use the whole record
        val vars = Try((z._2 \ "vars").get) match {
          case Success(s) => s
          case Failure(_) => z._2
        }
        // BI does not have period, so use an empty string
        val period = Try((z._1 \ "period").get) match {
          case Success(s) => s
          case Failure(_) => Json.toJson("")
        }
        val j = Json.obj(
          "id" -> (z._1 \ "id").get,
          "parents" -> (z._1 \ "parents").get,
          "children" -> (z._1 \ "children").get,
          "unitType" -> (z._1 \ "unitType").get,
          "period" -> period,
          "vars" -> vars
        )
        j
      }
    )
    Json.toJson(a)
  }
}
