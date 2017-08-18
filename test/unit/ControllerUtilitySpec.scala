package unit

import controllers.v1.ControllerUtils
import play.api.libs.json.JsNumber
import play.api.mvc.Result
import resource.TestUtils

/**
 * Created by haqa on 11/08/2017.
 */
class ControllerUtilitySpec extends TestUtils with ControllerUtils {


  "tryAsResponse" should {
    "return a acceptable Result object" in {
      val tryResponse = tryAsResponse[String](toJsonTest, "1234")
      tryResponse mustBe a[Result]
      tryResponse.header.status mustEqual OK
    }
    "execute a failure if the passed function fails in the try" in {
      val failedTry = tryAsResponse[String](toJsonTest, "The is not parsable as an Int")
      failedTry mustBe a[Result]
      noException should be thrownBy failedTry
      failedTry.header.status mustEqual BAD_REQUEST
    }
  }


  def toJsonTest(s: String) = JsNumber(s.toInt)

}
