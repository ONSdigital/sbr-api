package uk.gov.ons.sbr.models

import io.swagger.annotations.ApiModelProperty
import utils.Mapping
import config.Properties.{ minKeyLength, controlEndpoint, host }
import play.api.libs.json._
import uk.gov.ons.sbr.models.attributes.Address

/**
 * Created by Ameen on 15/07/2017.
 */
case class Enterprise(
  @ApiModelProperty(value = "", example = "", required = false, hidden = false) name: String,
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Long") id: Long,
  @ApiModelProperty(value = "", example = "") legalUnits: Seq[Long],
  @ApiModelProperty(dataType = "Address") address: Address,
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") legalStatus: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") sic: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") employees: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") workingGroup: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") employment: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Long") turnover: Option[Long],
  unitType: String
) extends Searchkeys[Long]

object Enterprise extends Mapping[Enterprise, Map[String, String]] {

  implicit val unitFormat: OFormat[Enterprise] = Json.format[Enterprise]

  def fromMap(values: Map[String, String]): Enterprise =
    Enterprise(values("name"), values("enterprise").toLong, filter(values),
      Address(Option(values("address1")), Option(values("address2")), Option(values("address3")),
        Option(values("address4")), Option(values("address5")), Option(values("postcode"))),
      Option(values("legalstatus").toInt), Option(values("sic").toInt),
      Option(values("employees").toInt), Option(values("workinggroup").toInt),
      Option(values("employment").toInt), Option(values("turnover").toLong), values("source"))

  def filter(values: Map[String, String]): Seq[Long] = {
    val res = Seq(values("legalunit1"), values("legalunit2"), values("legalunit3"), values("legalunit4")).map {
      case x if x.length > minKeyLength => Option(x.toLong)
      case _ => None
    }
    res.flatten
  }

  def toJson(e: List[Enterprise]): JsValue = Json.toJson(e)

}
