package uk.gov.ons.sbr.models

import play.api.libs.json.{ Json, OFormat }

/**
 * Created by coolit on 16/04/2018.
 */
case class LocalUnit(
  lurn: String,
  luref: Option[String],
  name: String,
  tradingStyle: Option[String],
  sic07: String,
  employees: Int,
  enterprise: LocalUnitEnterprise,
  address: LocalUnitAddress
)

object LocalUnit {
  implicit val unitFormat: OFormat[LocalUnit] = Json.format[LocalUnit]
}

case class LocalUnitAddress(
  line1: String,
  line2: String,
  line3: String,
  line4: String,
  line5: String,
  postcode: String
)

object LocalUnitAddress {
  implicit val unitFormat: OFormat[LocalUnitAddress] = Json.format[LocalUnitAddress]
}

case class LocalUnitEnterprise(
  ern: String,
  entref: String
)

object LocalUnitEnterprise {
  implicit val unitFormat: OFormat[LocalUnitEnterprise] = Json.format[LocalUnitEnterprise]
}