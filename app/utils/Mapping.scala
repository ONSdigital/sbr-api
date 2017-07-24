package utils

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

  /**
   *
   * @note replace r with gen. dt
   *       won't always use List type => gen.
   *       array => map
   */
  def toString(f: T => Map[String, Any], r: List[T]): String =
    r.map(z => JSONObject(f(z))).mkString("[", delim, "]")

  /**
   *
   * @note -  K could in fact be T such that K is not needed -> rep. T => K
   *       Address doe not extend Mapping -> thus no T,Z and declaration is needed.
   */
  def toJson[K](i: K, f: K => Map[String, String]) = JSONObject(f(i))

}