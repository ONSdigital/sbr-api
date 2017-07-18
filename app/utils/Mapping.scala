package utils

/**
  * Created by haqa on 18/07/2017.
  */
trait Mapping[T] {
//  def toMapfromArray(t: T) :
  def toMap(t: T): Map[String,Any]
  def fromMap(b: Map[String,Any]): T
}