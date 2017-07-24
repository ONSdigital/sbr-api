package utils

import utils.Utilities.errAsJson

import scala.util.parsing.json.JSONObject

/**
 * Created by haqa on 18/07/2017.
 */

/**
 *
 * @tparam T - SearchKeys object type
 * @tparam Z - type of ds keys are stored
 */
trait Mapping[T, Z] {

  private val delim: String = ","

  def toMap(m: T): Map[String, Any]

  def fromMap(b: Z): T

  def filter(x: Z): AnyRef

  @deprecated("Moved to new toString", "devops/jenkins [Mon 24 July 2017 - 10:15]")
  def toString2(f: T => Map[String, Any], returned: List[T]): String = returned.map {
    case z => JSONObject(f(z))
    case _ => errAsJson(404, "missing rec", "Cannot find data in rec").toString
  }.map(x => s"""$x""").mkString("[", delim, "]")

  def toString(f: T => Map[String, Any], returned: List[T]): String =
    returned.map(z => JSONObject(f(z))).mkString("[", delim, "]")

  /**
   *
   * @note -  K could in fact be T such that K is not needed -> rep. T => K
   *       Address doe not extend Mapping -> thus no T,Z and declaration is needed.
   */
  def toJson[K](i: K, f: K => Map[String, String]) = JSONObject(f(i))

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