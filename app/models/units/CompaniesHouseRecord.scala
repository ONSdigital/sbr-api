package models.units

import io.swagger.annotations.ApiModelProperty
import models.units.attributes.Address

/**
 * Created by Ameen on 15/07/2017.
 */
case class CompaniesHouseRecord(
  id: String,
  @ApiModelProperty(dataType = "Address") address: Address,
  postcode: String,
  source: String = "CRN"

) extends Searchkeys[String]

