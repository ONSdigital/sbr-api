package utils

import io.swagger.annotations.ApiModelProperty
import models.units.{Enterprise, Searchkeys}
import models.units.attributes.{Address, AddressObj, Matches}
import utils.Utilities.{errAsJson, getElement}

import scala.util.parsing.json.JSONObject
/**
 * Created by haqa on 07/07/2017.
 */

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

  def fromMap(values: Array[String]): Enterprise =
    Enterprise(values(0), values(1).toLong, values(2), Address(values(3), values(4), values(5), values(6), values(7)),
      values(8), Option(values(9).toInt), Option(values(10).toInt), Option(values(11).toInt), Option(values(12).toInt),
      Option(values(13).toInt), Option(values(14).toLong), values(15))

  def fetch(elem: Any) = elem match {
    case (a: Address) => JSONObject(AddressObj.toMap(a))
    case _ => getElement(elem)
  }


  def ccToMap(cc: AnyRef) =
    (Map[String, Any]() /: cc.getClass.getDeclaredFields) {
      (a, f) =>
        f.setAccessible(true)
        a + (f.getName -> f.get(cc))
    }

  def createCC(values: Array[String], x: AnyRef) = ???


}
