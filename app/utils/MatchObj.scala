package utils

import models.units.{ Enterprise }
import models.units.attributes.{ Address, AddressObj }
import utils.Utilities.{ errAsJson, getElement }

import scala.util.parsing.json.JSONObject
/**
 * Created by haqa on 07/07/2017.
 */

@deprecated("Migrated to Mapping", "alpha/link [Wed 19 July 2017 - 11:15]")
object MatchObj {

  private val delim: String = ","

  def toMap(m: Enterprise) = Map(
    "name" -> m.name,
    "enterprise" -> m.id,
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

  def toString(returned: List[Enterprise]): String = returned.map {
    case z => s"""${toMap(z).map(x => s""""${x._1}":${fetch(x._2)}""").mkString(delim)}"""
    case _ => errAsJson(404, "missing field", "Cannot find data in field")
  }.map(x => s"""{$x}""").mkString("[", delim, "]")

  @SuppressWarnings(Array("unused"))
  def fromMap(values: Array[String]): Enterprise =
    Enterprise(values(0), values(1).toLong, Seq(Option(values(2).toLong), Option(values(3).toLong), Option(values(4).toLong),
      Option(values(5).toLong)), Address(values(6), values(7), values(8), values(9), values(10)), values(11),
      Option(values(12).toInt), Option(values(13).toInt), Option(values(14).toInt), Option(values(15).toInt),
      Option(values(16).toInt), Option(values(17).toLong))

  def fetch(elem: Any) = elem match {
    case (a: Address) => JSONObject(AddressObj.toMap(a))
    case _ => getElement(elem)
  }

}
