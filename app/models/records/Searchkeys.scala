package models.records

/**
 * Created by haqa on 11/07/2017.
 */
trait Searchkeys[T] {
  // enterprise -> id
  // def id: T
  def enterprise: Option[T]
  def source: String
}

