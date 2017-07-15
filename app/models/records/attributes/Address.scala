package models.records.attributes

import io.swagger.annotations.ApiModelProperty

/**
 * Created by Ameen on 15/07/2017.
 */
case class Address(
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.String") line_1: Option[String],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.String") line_2: Option[String],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.String") line_3: Option[String],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.String") line_4: Option[String],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.String") line_5: Option[String]
)
