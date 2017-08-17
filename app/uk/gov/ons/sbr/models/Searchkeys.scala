package uk.gov.ons.sbr.models

import com.google.inject.ImplementedBy
import io.swagger.annotations.ApiModelProperty
import uk.gov.ons.sbr.models.attributes.Address

/**
 * Created by haqa on 11/07/2017.
 */
@ImplementedBy(classOf[Enterprise])
trait Searchkeys[T] {
  @ApiModelProperty(value = "A searchable identifier", required = true, hidden = false) def id: T
  @ApiModelProperty(value = "An address object consisting of 5 line descriptors") def address: Address
  //  @ApiModelProperty(value = "A key value pair of all variables associated", example = "",
  //    dataType = "Map[String,String]") def variables: Map[String, String]
  //  @ApiModelProperty(value = "Date Created") def period: String
  @ApiModelProperty(value = "Data provider") def unitType: String
}

object UnitTypes {

  // abbreviations
  val LegalUnitType = "LEU"
  val EnterpriseUnitType = "ENT"
  val PayAsYouEarnUnitType = "PAYE"
  val ValueAddedTaxUnitType = "VAT"
  val CompanyRegistrationNumberUnitType = "CRN"

}