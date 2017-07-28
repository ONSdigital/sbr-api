package utils

import play.api.libs.json.JsValue

/**
 * Created by haqa on 18/07/2017.
 */

/**
 * @tparam T - SearchKeys object type
 * @tparam Z - type of ds keys are stored
 */
trait Mapping[T, Z] {

  def fromMap(b: Z): T

  def filter(x: Z): AnyRef

  def toJson(x: List[T]): JsValue


}