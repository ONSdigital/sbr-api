package utils

import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._

import uk.gov.ons.sbr.models.UnitTypesShortNames._

object UriBuilder {

  private val periodPath = "periods"
  private val typePath = "types"
  private val unitPath = "units"
  private val enterprisePath = "enterprises"
  private val leuPath = "business"
  private val crnPath = "companies"
  private val suffix = "s"

  def uriPathBuilder(baseUrl: String, units: String, periods: Option[String] = None, types: Option[String] = None,
    group: String = ""): Uri = {
    //    val unitTypePath = if (group.isEmpty) { unitPath } else if (group == "ent") { enterprisePath } else { group + "s" }
    val unitTypePath = group.toUpperCase match {
      case LEGAL_UNIT_TYPE => leuPath
      case COMPANIES_HOUSE_REFERENCE_NUMBER_TYPE => crnPath
      case PAYE_TYPE => group + suffix
      case VAT_REFERENCE_TYPE => group + suffix
      case ENTERPRISE_TYPE => enterprisePath
      case _ => unitPath
    }
    (periods, types, units) match {
      case (Some(x), Some(y), z) => baseUrl / periodPath / x / typePath / y / unitTypePath / z
      case (Some(x), None, z) => baseUrl / periodPath / x / unitTypePath / z
      case (None, Some(y), z) => baseUrl / typePath / y / unitTypePath / z
      case _ => baseUrl / unitTypePath / units
    }
  }

}
