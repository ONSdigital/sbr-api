package unit

import play.api.libs.json.JsNumber
import play.api.mvc.Result

import controllers.v1.ControllerUtils
import resource.TestUtils

/**
 * Created by haqa on 11/08/2017.
 */
class ControllerUtilitySpec extends TestUtils with ControllerUtils {

  def toJsonTest(s: String) = JsNumber(s.toInt)

}
