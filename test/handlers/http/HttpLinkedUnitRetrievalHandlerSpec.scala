package handlers.http

import java.time.Month.FEBRUARY

import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FreeSpec, Matchers, OptionValues }
import play.api.http.MimeTypes.JSON
import play.api.libs.json.{ JsObject, JsString, Writes }
import play.api.test.Helpers._
import support.sample.SampleLinkedUnit
import uk.gov.ons.sbr.models._

import scala.concurrent.Future

class HttpLinkedUnitRetrievalHandlerSpec extends FreeSpec with Matchers with MockFactory with OptionValues {

  private trait Fixture {
    val ALinkedUnit = SampleLinkedUnit.forEnterprise(Period.fromYearMonth(2018, FEBRUARY), Ern("1234567890"))
    val LinkedUnitJsonRepresentation = JsObject(Seq("foo" -> JsString("bar")))

    val writesLinkedUnit = mock[Writes[LinkedUnit]]
    val httpHandler = new HttpLinkedUnitRetrievalHandler(writesLinkedUnit)
  }

  "A HTTP handler" - {
    "returns a JSON representation of the retrieved LinkedUnit when found" in new Fixture {
      (writesLinkedUnit.writes _).expects(ALinkedUnit).returning(LinkedUnitJsonRepresentation)

      val result = httpHandler(Right(Some(ALinkedUnit)))

      val futResult = Future.successful(result)
      status(futResult) shouldBe OK
      contentType(futResult).value shouldBe JSON
      contentAsJson(futResult) shouldBe LinkedUnitJsonRepresentation
    }

    "returns NOT_FOUND when the retrieval cannot find either the unit or its associated links" in new Fixture {
      val result = httpHandler(Right(None))

      result.header.status shouldBe NOT_FOUND
    }

    "returns GATEWAY_TIMEOUT when the retrieval time exceeds the configured time out" in new Fixture {
      val result = httpHandler(Left("Timeout"))

      result.header.status shouldBe GATEWAY_TIMEOUT
    }

    "returns INTERNAL_SERVER_ERROR when the retrieval fails" in new Fixture {
      val result = httpHandler(Left("Retrieval failed"))

      result.header.status shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
