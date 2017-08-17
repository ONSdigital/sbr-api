package uk.gov.ons.sbr.models

import io.swagger.annotations.ApiModelProperty
import play.api.libs.json._
import uk.gov.ons.sbr.models.attributes.Address
import utils.Mapping

/**
 * Created by haqa on 31/07/2017.
 */
case class VatRef(
  id: Long,
  @ApiModelProperty(dataType = "Address") address: Address,
  postcode: String,
  sic: Int,
  turnover: Long,
  legalStatus: Int,
  crn: String,
  unitType: String = "VatRef"
) extends Searchkeys[Long]

object VatRef extends Mapping[VatRef, Map[String, String]] {

  implicit val unitFormat = Json.format[VatRef]

  def fromMap(b: Map[String, String]): VatRef = ???

  def filter(x: Map[String, String]): AnyRef = ???

  def toJson(x: List[VatRef]): JsValue = ???

}
