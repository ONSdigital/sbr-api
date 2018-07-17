package repository.rest

import org.scalamock.scalatest.MockFactory
import org.scalatest.{ EitherValues, FreeSpec, Matchers }
import play.api.libs.json.{ JsError, JsSuccess, Json, Reads }

class RepositoryResultSpec extends FreeSpec with Matchers with MockFactory with EitherValues {

  private trait Fixture {
    case class DummyValue(field: String)

    val DummyResult = DummyValue("foo")
    val JsonResult = Json.parse(s"""{"answer":42}""")

    val readsDummyValue = mock[Reads[DummyValue]]
    val unitRepositoryResult = RepositoryResult.as[DummyValue](readsDummyValue) _
  }

  "A UnitRepositoryResult" - {
    "when a success" - {
      "can be converted from json when it contains a result value" - {
        "returning the conversion result when conversion is successful" in new Fixture {
          (readsDummyValue.reads _).expects(JsonResult).returning(JsSuccess[DummyValue](DummyResult))

          unitRepositoryResult(Right(Some(JsonResult))).right.value shouldBe Some(DummyResult)
        }

        "returning a failure when conversion fails" in new Fixture {
          (readsDummyValue.reads _).expects(JsonResult).returning(JsError("conversion failure"))

          unitRepositoryResult(Right(Some(JsonResult))).left.value should startWith("Unable to parse json response")
        }
      }

      "returns None when there is no result value" in new Fixture {
        unitRepositoryResult(Right(None)).right.value shouldBe empty
      }
    }

    "when a failure" - {
      "retains the original failure message" in new Fixture {
        val failureMessage = "retrieval failed"

        unitRepositoryResult(Left(failureMessage)).left.value shouldBe failureMessage
      }
    }
  }
}
