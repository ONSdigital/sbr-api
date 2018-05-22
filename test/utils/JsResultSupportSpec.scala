package utils

import org.scalatest.{ FreeSpec, Matchers }
import play.api.libs.json.{ JsError, JsSuccess }

import scala.util.{ Failure, Success }

class JsResultSupportSpec extends FreeSpec with Matchers {

  "A JsResult" - {
    "can be created from a Try" - {
      "when a Success" in {
        val value = "hello world!"

        JsResultSupport.fromTry(Success(value)) shouldBe JsSuccess(value)
      }

      "when a Failure" in {
        val cause = new Exception("failure message")

        JsResultSupport.fromTry(Failure(cause)) shouldBe JsError(cause.getMessage)
      }
    }

    "can be created from an Option" - {
      "when a Some indicates a success value" in {
        val value = "hello world!"

        JsResultSupport.fromOption(Some(value)) shouldBe JsSuccess(value)
      }

      "when a None indicates a failure to obtain a value" in {
        JsResultSupport.fromOption(None) shouldBe JsError()
      }
    }
  }
}
