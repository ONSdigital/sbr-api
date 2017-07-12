package controllers.v1

import io.swagger.annotations.ApiModelProperty

/**
 * Created by haqa on 11/07/2017.
 */
sealed trait Searchkeys[T] {
  // enterprise -> id
  // def id: T
  def enterprise: Option[T]
  def source: String
}

final case class Matches(
  @ApiModelProperty(value = "", example = "", required = false, hidden = false) name: String,
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.String") enterprise: Option[String],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.String") paye: Option[String],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Long") vatref: Option[Long],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Long") ubrn: Option[Long],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.String") crn: Option[String],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Long") idbr: Option[Long],
  @ApiModelProperty(value = "", example = "") address1: String,
  @ApiModelProperty(value = "", example = "") address2: String,
  @ApiModelProperty(value = "", example = "") address3: String,
  @ApiModelProperty(value = "", example = "") address4: String,
  @ApiModelProperty(value = "", example = "") address5: String,
  @ApiModelProperty(value = "", example = "") postcode: String,
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") legalStatus: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") sic: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") employees: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") workingGroup: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") employment: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Long") turnover: Option[Long],
  @ApiModelProperty(value = "", example = "") source: String
) extends Searchkeys[String]
