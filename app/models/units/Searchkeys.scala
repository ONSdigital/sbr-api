package models.units

import models.units.attributes.{ Address }

/**
 * Created by haqa on 11/07/2017.
 */
trait Searchkeys[T] {
  // rep. enterprise -> id
  //  def id: T
  def enterprise: Option[T]
    def address: Address
    def postcode: String
  def source: String
}

