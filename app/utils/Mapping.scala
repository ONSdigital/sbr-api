package utils

import models.units.{ Enterprise, EnterpriseObj }
import models.units.attributes.{ Address, AddressObj }
import utils.Utilities.{ errAsJson, getElement }

import scala.util.parsing.json.JSONObject

/**
 * Created by haqa on 18/07/2017.
 */
trait Mapping[T, Z] {

  private val delim: String = ","

  def toMap(t: T): Map[String, Any]
  def fromMap(b: Z): T

  def toString(returned: List[Enterprise]): String = returned.map {
    case z => s"""${EnterpriseObj.toMap(z).map(x => s""""${x._1}":${fetch(x._2)}""").mkString(delim)}"""
    case _ => errAsJson(404, "missing field", "Cannot find data in field")
  }.map(x => s"""{$x}""").mkString("[", delim, "]")

  def fetch(elem: Any) = elem match {
    case (a: Address) => JSONObject(AddressObj.toMap(a))
    case _ => getElement(elem)
  }

  @SuppressWarnings(Array("unused"))
  def ccToMap(cc: AnyRef) =
    (Map[String, Any]() /: cc.getClass.getDeclaredFields) {
      (a, f) =>
        f.setAccessible(true)
        a + (f.getName -> f.get(cc))
    }

  @SuppressWarnings(Array("unused"))
  def createCC(values: Array[String], x: AnyRef) = ???

}