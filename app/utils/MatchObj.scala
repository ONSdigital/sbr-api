package utils

import models.units.attributes.{ Address, AddressObj, Matches }
import utils.Utilities.{ errAsJson, getElement }

import scala.util.parsing.json.JSONObject
/**
 * Created by haqa on 07/07/2017.
 */

object MatchObj {

  private val delim: String = ","

  def toMap(m: Matches) = Map(
    "name" -> m.name,
    "enterprise" -> m.id,
    "paye" -> m.paye,
    "vatref" -> m.vatref,
    "ubrn" -> m.ubrn,
    "crn" -> m.crn,
    "idbr" -> m.idbr,
    "address" -> m.address,
    "postcode" -> m.postcode,
    "legalStatus" -> m.legalStatus,
    "sic" -> m.sic,
    "employees" -> m.employees,
    "workingGroup" -> m.workingGroup,
    "employment" -> m.employment,
    "turnover" -> m.turnover,
    "source" -> m.source
  )

  def toString(returned: List[Matches]): String = returned.map {
    case z => s"""${toMap(z).map(x => s""""${x._1}":${fetch(x._2)}""").mkString(delim)}"""
    case _ => errAsJson(404, "missing field", "Cannot find data in field")
  }.map(x => s"""{$x}""").mkString("[", delim, "]")

  def fromMap(values: Array[String]): Matches =
    Matches(values(0), values(1), Option(values(2)), Option(values(3).toLong), Option(values(4).toLong),
      Option(values(5)), Option(values(6).toLong), Address(values(7), values(8), values(9), values(10), values(11)), values(12),
      Option(values(13).toInt), Option(values(14).toInt), Option(values(15).toInt), Option(values(16).toInt),
      Option(values(17).toInt), Option(values(18).toLong), values(19))

  def fetch(elem: Any) = elem match {
    case (a: Address) => JSONObject(AddressObj.toMap(a))
    case _ => getElement(elem)
  }

}
