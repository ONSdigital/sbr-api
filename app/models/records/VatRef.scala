package models.records

/**
 * Created by Ameen on 15/07/2017.
 */
case class VatRef(
  enterprise: Option[String],
  source: String

) extends Searchkeys[String]
