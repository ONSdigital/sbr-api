package uk.gov.ons.sbr.models

import io.swagger.annotations.ApiModelProperty
import play.api.libs.json._
import uk.gov.ons.sbr.models.attributes.Address

/**
 * Created by haqa on 02/08/2017.
 */

case class LegalUnit(
  @ApiModelProperty(value = "Unit identifier", example = "", dataType = "java.lang.Long", required = true, hidden = false) id: Long,
  @ApiModelProperty(dataType = "Address") address: Address,
  variables: Map[String, String],
  period: String = "Unavailable",
  unitType: String = "Legal Unit"
) extends Searchkeys[Long]

object LegalUnit {

  implicit val legalUnitFormat: OFormat[LegalUnit] = Json.format[LegalUnit]

  protected def apply(j: JsValue): LegalUnit =
    LegalUnit((j \ "id").as[Long], Address(None, None, None, None, None, Some((j \ "postCode").as[String])),
      Map(
        "businessName" -> (j \ "businessName").as[String],
        "industryCode" -> (j \ "industryCode").as[String],
        "legalStatus" -> (j \ "legalStatus").as[String],
        "tradingStatus" -> (j \ "tradingStatus").as[String],
        "turnover" -> (j \ "turnover").as[String],
        "employmentBands" -> (j \ "employmentBands").as[String],
        "companyNo" -> (j \ "companyNo").as[String]
      )
    //      (j \ "period").as[String]
    )

  protected def parse(jsonString: String) = Json.parse(jsonString)

  def toJson(x: String): JsValue = Json.toJson(apply(parse(x)))

}

