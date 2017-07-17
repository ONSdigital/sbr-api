package models.units

import models.units.attributes.{ Address }

/**
 * Created by Ameen on 15/07/2017.
 */
case class CompaniesHouseRecord(
  enterprise: Option[String],
  source: String,
  postcode: String,
  address : Address

) extends Searchkeys[String]
