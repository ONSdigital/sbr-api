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
) {
  // FIX replace with JsObject <- JsValue
  override def toString: String = s"""{"address_line_1":"${addressLine1}","address_line_2":"${addressLine2}","address_line_3":"${addressLine3}","address_line_4":"${addressLine4}","address_line_5":"${addressLine5}"}"""
}