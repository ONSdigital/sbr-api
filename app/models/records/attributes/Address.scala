package models.records.attributes

import io.swagger.annotations.ApiModelProperty

/**
 * Created by Ameen on 15/07/2017.
 */
case class Address(
  @ApiModelProperty(value = "Street and optional door number", example = "101 Long Street") addressLine1: String,
  @ApiModelProperty(value = "Optional field for building or apartment name", example = "Little Winsor") addressLine2: String,
  @ApiModelProperty(value = "Town of address", example = "Bury") addressLine3: String,
  @ApiModelProperty(value = "City of address", example = "Manchester") addressLine4: String,
  @ApiModelProperty(value = "County of address", example = "Gtr Manchester") addressLine5: String
)

object AddressObj {
  def toMap(a: Address): Map[String, String] = Map(
    "addressLine1" -> a.addressLine1,
    "addressLine2" -> a.addressLine2,
    "addressLine3" -> a.addressLine3,
    "addressLine4" -> a.addressLine4,
    "addressLine5" -> a.addressLine5
  )
}