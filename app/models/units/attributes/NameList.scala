package models.units.attributes

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
object NameList {

}
