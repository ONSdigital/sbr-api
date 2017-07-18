package models.units

import models.units.attributes.Address

/**
 * Created by Ameen on 15/07/2017.
 */
case class IDBR(
  id: String,
  address: Address,
  postcode: String,
  source: String = "IDBR"

) extends Searchkeys[String]
