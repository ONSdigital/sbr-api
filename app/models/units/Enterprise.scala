package models.units

import io.swagger.annotations.ApiModelProperty
import models.units.attributes.{ Address }
import utils.Mapping
import utils.Properties.{ minKeyLength }
import play.api.libs.json._

/**
 * Created by Ameen on 15/07/2017.
 */
case class Enterprise(
  @ApiModelProperty(value = "", example = "", required = false, hidden = false) name: String,
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Long") id: Long,
  @ApiModelProperty(value = "", example = "") legalUnits: Seq[Long],
  @ApiModelProperty(dataType = "Address") address: Address,
  postcode: String,
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") legalStatus: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") sic: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") employees: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") workingGroup: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") employment: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Long") turnover: Option[Long],
  source: String = "Enterprise"
) extends Searchkeys[Long]

object Enterprise extends Mapping[Enterprise, Map[String, String]] {

  implicit val unitFormat: OFormat[Enterprise] = Json.format[Enterprise]

  def fromMap(values: Map[String, String]): Enterprise =
    Enterprise(values("name"), values("enterprise").toLong, filter(values),
      Address(values("address1"), values("address2"), values("address3"),
        values("address4"), values("address5")), values("postcode"),
      Option(values("legalstatus").toInt), Option(values("sic").toInt),
      Option(values("employees").toInt), Option(values("workinggroup").toInt),
      Option(values("employment").toInt), Option(values("turnover").toLong))

  def filter(values: Map[String, String]): Seq[Long] = {
    val res = Seq(values("legalunit1"), values("legalunit2"), values("legalunit3"), values("legalunit4")).map {
      case x if x.length > minKeyLength => Option(x.toLong)
      case _ => None
    }
    res.flatten
  }

  def toJson(e: List[Enterprise]): JsValue = Json.toJson(e.head)

}
