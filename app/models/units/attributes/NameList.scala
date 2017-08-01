package models.units.attributes

import play.api.libs.json.{JsValue, Json}
import utils.Mapping

/**
 * Created by haqa on 31/07/2017.
 */

case class NameList(
  vat: Option[List[Long]],
  paye: Option[List[String]],
  crn: Option[List[String]],
  ubrn: Option[List[Long]],
  enterprise: Option[List[Long]]
)
object NameList extends Mapping [NameList, Map[String, String]]{

  implicit val unitFormat = Json.format[NameList]

  def fromMap(b: Map[String, String]): NameList = ???

  def filter(x: Map[String, String]): AnyRef = ???

  def toJson(x: List[NameList]): JsValue = ???


}
