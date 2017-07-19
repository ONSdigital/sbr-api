package models.units

import io.swagger.annotations.ApiModelProperty
import models.units.attributes.Address
import utils.Mapping

/**
 * Created by Ameen on 15/07/2017.
 */
final case class Enterprise(
  @ApiModelProperty(value = "", example = "", required = false, hidden = false) name: String,
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Long") id: Long,
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.String") legalUnit: Long,
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

object EnterpriseObj extends Mapping[Enterprise, Array[String]] {

  def toMap(e: Enterprise): Map[String, Any] = Map(
    "name" -> e.name,
    "id" -> e.id,
    "legalUnit" -> e.legalUnit,
    "address" -> e.address,
    "postcode" -> e.postcode,
    "legalStatus" -> e.legalStatus,
    "sic" -> e.sic,
    "employees" -> e.employees,
    "workingGrouping" -> e.workingGroup,
    "employment" -> e.employment,
    "turnover" -> e.turnover
  )

  def fromMap(values: Array[String]): Enterprise =
    Enterprise(values(0), values(1).toLong, values(2).toLong, Address(values(3), values(4), values(5), values(6), values(7)),
      values(8), Option(values(9).toInt), Option(values(10).toInt), Option(values(11).toInt), Option(values(12).toInt),
      Option(values(13).toInt), Option(values(14).toLong))

}
