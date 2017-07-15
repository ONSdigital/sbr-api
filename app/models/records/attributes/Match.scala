package models.records.attributes

import io.swagger.annotations.ApiModelProperty
import models.records.Searchkeys

/**
 * Created by Ameen on 15/07/2017.
 */

final case class Matches(
  @ApiModelProperty(value = "", example = "", required = false, hidden = false) name: String,
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.String") enterprise: Option[String],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.String") paye: Option[String],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Long") vatref: Option[Long],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Long") ubrn: Option[Long],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.String") crn: Option[String],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Long") idbr: Option[Long],
  @ApiModelProperty(value = "", example = "") address: Address,
  @ApiModelProperty(value = "", example = "") postcode: String,
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") legalStatus: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") sic: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") employees: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") workingGroup: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") employment: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Long") turnover: Option[Long],
  @ApiModelProperty(value = "", example = "") source: String
) extends Searchkeys[String]

