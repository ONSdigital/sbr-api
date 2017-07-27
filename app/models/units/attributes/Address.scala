package models.units.attributes

import io.swagger.annotations.ApiModelProperty

import scala.util.parsing.json.JSONObject

/**
 * Created by Ameen on 15/07/2017.
 */
case class Address(
  @ApiModelProperty(value = "Street and optional door number", example = "101 Long Street") line1: String,
  @ApiModelProperty(value = "Optional field for building or apartment name", example = "Little Winsor") line2: String,
  @ApiModelProperty(value = "Town of address", example = "Bury") line3: String,
  @ApiModelProperty(value = "City of address", example = "Manchester") line4: String,
  @ApiModelProperty(value = "County of address", example = "Gtr Manchester") line5: String
)

object AddressObj {
  def toMap(a: Address): Map[String, String] = Map(
    "line1" -> a.line1,
    "line2" -> a.line2,
    "line3" -> a.line3,
    "line4" -> a.line4,
    "line5" -> a.line5
  )

  def toJson(a: Address) = JSONObject(toMap(a))

}

