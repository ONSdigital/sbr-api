package models.units

import io.swagger.annotations.ApiModelProperty
import play.api.libs.json._
import utils.Utilities.getElement

/**
 * Created by haqa on 02/08/2017.
 */

case class LegalUnit(
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Long") id: Long,
  @ApiModelProperty(value = "", example = "", required = false, hidden = false) businessName: String,
  //  @ApiModelProperty(dataType = "Address") address: Address,
  @ApiModelProperty(dataType = "Address") postCode: String,
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") industryCode: Option[String],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") legalStatus: Option[String],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") tradingStatus: Option[String],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") turnover: Option[String],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") employmentBands: Option[String],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.String") companyNo: Option[String],
  source: String = "Legal Unit"

)

//val t = response.json.as (Seq (JsObject) )
object LegalUnit {
  //  implicit val writeLegal = Json.writes[LegalUnit]
  implicit val legalUnitFormat: OFormat[LegalUnit] = Json.format[LegalUnit]

  def toMap(j: JsValue): Map[String, Any] = Map(
    "id" -> getElement(j \ "id"),
    "businessName" -> (j \ "businessName").as[String],
    "postCode" -> (j \ "postCode").as[String],
    "industryCode" -> (j \ "industryCode").as[String],
    "legalStatus" -> (j \ "legalStatus").as[String],
    "tradingStatus" -> (j \ "tradingStatus").as[String],
    "turnover" -> (j \ "turnover").as[String],
    "employmentBands" -> (j \ "employmentBands").as[String],
    "companyNo" -> (j \ "companyNo").as[String],
    "source" -> "Legal Unit"
  )

  def fromMap(values: Map[String, Any]): LegalUnit =
    LegalUnit(values("id").toString.toLong, values("businessName").toString, values("postCode").toString,
      values.get("industryCode").map(_.toString), values.get("legalStatus").map(_.toString), values.get("tradingStatus").map(_.toString),
      values.get("turnover").map(_.toString), values.get("employmentBands").map(_.toString), values.get("companyNo").map(_.toString),
      values("source").toString)

  def parse(jsonString: String) = Json.parse(jsonString)

  def toJson(x: String): JsValue = Json.toJson(fromMap(toMap(parse(x))))

}

