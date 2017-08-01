package models.units.attributes

import io.swagger.annotations.ApiModelProperty
import models.units.{Searchkeys}
import play.api.libs.json.{JsValue, Json}
import utils.Mapping

/**
  * Created by Ameen on 15/07/2017.
  */

case class Match(
                    @ApiModelProperty(value = "", example = "", required = false, hidden = false) name: String,
                    id: String,
                    @ApiModelProperty(value = "", example = "", dataType = "java.lang.String") paye: Option[String],
                    @ApiModelProperty(value = "", example = "", dataType = "java.lang.Long") vatref: Option[Long],
                    @ApiModelProperty(value = "", example = "", dataType = "java.lang.Long") ubrn: Option[Long],
                    @ApiModelProperty(value = "", example = "", dataType = "java.lang.String") crn: Option[String],
                    @ApiModelProperty(value = "", example = "", dataType = "java.lang.Long") idbr: Option[Long],
                    @ApiModelProperty(dataType = "classOf[Address]") address: Address,
                    postcode: String,
                    @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") legalStatus: Option[Int],
                    @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") sic: Option[Int],
                    @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") employees: Option[Int],
                    @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") workingGroup: Option[Int],
                    @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") employment: Option[Int],
                    @ApiModelProperty(value = "", example = "", dataType = "java.lang.Long") turnover: Option[Long],
                    source: String
                  ) extends Searchkeys[String]

object Match extends Mapping[Match, Map[String, String]]{
  implicit val unitFormat = Json.format[Match]

  def fromMap(b: Map[String, String]): Match = ???

  def filter(x: Map[String, String]): AnyRef = ???

  def toJson(x: List[Match]): JsValue = ???

}
