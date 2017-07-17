package models.units

/**
 * Created by Ameen on 15/07/2017.
 */
case class CompaniesHouseRecord(
  enterprise: Option[String],
  source: String

) extends Searchkeys[String]
