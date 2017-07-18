package models.units

import io.swagger.annotations.ApiModelProperty
import models.units.attributes.Address

/**
 * Created by Ameen on 15/07/2017.
 */
case class LegalUnit(
  @ApiModelProperty(value = "A unique business id", dataType = "java.lang.Long", example = "85282744", required = true) id: Long, // the same as uprn ?
  @ApiModelProperty(value = "The Name of the Business or Company", dataType = "String", example = "BI (2018) LIMITED", required = true) businessName: String,
  @ApiModelProperty(value = "A unique property number identifier", dataType = "java.lang.Long", example = "977146940701", required = false) uprn: Option[Long],
  @ApiModelProperty(dataType = "Address") address: Address,
  @ApiModelProperty(example = "SE", required = false) postcode: String,
  @ApiModelProperty(value = "Industry Code representing the industry the business is in", dataType = "String", example = "62020", required = false) industryCode: Option[String],
  @ApiModelProperty(value = "The legal category the business falls within", dataType = "String", example = "1", required = false) legalStatus: Option[String],
  @ApiModelProperty(value = "The operational status of the business", dataType = "String", example = "A", required = false) tradingStatus: Option[String],
  @ApiModelProperty(value = "The turnover of the business", dataType = "String", example = "B", required = false) turnover: Option[String],
  @ApiModelProperty(value = "The number of employees the company employees in bands", dataType = "String", example = "E", required = false) employmentBands: Option[String],
  @ApiModelProperty(value = "The business's VAT Reference Number", required = false) vatRefs: Option[Seq[Long]] = None,
  @ApiModelProperty(value = "The business's PAYE Reference Number", required = false) payeRefs: Option[Seq[String]] = None,
  @ApiModelProperty(value = "The business's Company Number to be identified by Companies House", dataType = "String", example = "29531562", required = false) companyNo: Option[String] = None,
  source: String = "legalUnit"
) extends Searchkeys[Long]
