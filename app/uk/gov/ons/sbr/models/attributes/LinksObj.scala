package uk.gov.ons.sbr.models.attributes

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
object Link extends Mapping[Link, Array[String]] {

  implicit val unitFormat = ???

  def toJson(x: List[Link]) = ???

  private val relationshipMap: Map[String, Int] = Map(
    "CURRENT" -> 0,
    "GRANDPARENT" -> 1,
    "PARENT" -> 2,
    "CHILD" -> 3,
    "GRANDCHILD" -> 4
  )

  def fromMap(values: Array[String]): Link =
    Link(Option(values(relationshipMap("CURRENT"))), Option(values(relationshipMap("GRANDPARENT"))), Option(values(relationshipMap("PARENT"))),
      Option(values(relationshipMap("CHILD"))), Option(values(relationshipMap("GRANDCHILD"))))

  def filter(z: Array[String]): AnyRef = ???

}

