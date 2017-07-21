package models.units.attributes

import utils.Mapping

/**
 * Created by haqa on 19/07/2017.
 */
@deprecated("Unknown", "feature/ubrn-search [Fri 21 July 2017 - 09:02]")
final case class Link(
  current: Option[String],
  grandparent: Option[String],
  parent: Option[String],
  child: Option[String],
  grandchild: Option[String]
)

@deprecated("Unknown", "feature/ubrn-search [Fri 21 July 2017 - 09:02]")
object LinksObj extends Mapping[Link, Array[String]] {

  def toMap(v: Link): Map[String, Any] = Map(
    "current" -> v.current,
    "grandparent" -> v.grandparent,
    "parent" -> v.parent,
    "child" -> v.child,
    "grandchild" -> v.grandchild
  )

  def fromMap(values: Array[String]): Link =
    Link(Option(values(0).toString), Option(values(1).toString), Option(values(2).toString),
      Option(values(3).toString), Option(values(4).toString))

  def filter(z: Array[String]): AnyRef = ???

}

