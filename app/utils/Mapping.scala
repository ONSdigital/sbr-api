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

  def toMap(m: T): Map[String, Any]

  def fromMap(b: Z): T

  @deprecated("Moved to utils.toString", "feature/ubrn-search [Thu 20 July 2017 - 12:58]")
  def toString(returned: List[Enterprise]): String = returned.map {
    case z => s"""${EnterpriseObj.toMap(z).map(x => s""""${x._1}":${fetch(x._2)}""").mkString(delim)}"""
    case _ => errAsJson(404, "missing field", "Cannot find data in field")
  }.map(x => s"""{$x}""").mkString("[", delim, "]")

  /**
   *
   * @todo - deprecate getElement in this instance and remove fetch
   *       function by adapting the toMap function => use ++ cmd
   */
  def toString(f: T => Map[String, Any], returned: List[T]): String = returned.map {
    case z => s"""${f(z).map(x => s""""${x._1}":${fetch(x._2)}""").mkString(delim)}"""
    case _ => errAsJson(404, "missing field", "Cannot find data in field")
  }.map(x => s"""{$x}""").mkString("[", delim, "]")

  /**
   *
   * @note -  K could in fact be T
   *       Address doe not extend Mapping -> thus no T,Z and declaration is needed.
   */
  def toJson[K](i: K, f: K => Map[String, String]) = JSONObject(f(i))
  //  @SuppressWarnings(Array("unfinished"))
  //  def toJson(i: T, f: T => Map[String, String]) = JSONObject(f(i))

  def fetch(elem: Any) = elem match {
    case (a: Address) => toJson[Address](a, AddressObj.toMap)
    case _ => getElement(elem)
  }

  def filterChildren(x: Z): Any

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