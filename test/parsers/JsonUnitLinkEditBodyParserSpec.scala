package parsers

import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{EitherValues, FreeSpec, Matchers}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status._
import play.api.test.{FakeRequest, StubPlayBodyParsersFactory}
import play.mvc.Http.HeaderNames.CONTENT_TYPE
import play.mvc.Http.MimeTypes.{JSON, XML}
import uk.gov.ons.sbr.models._

import scala.concurrent.ExecutionContext

/*
 * See https://github.com/playframework/playframework/blob/master/framework/src/play/src/test/scala/play/mvc/RawBodyParserSpec.scala
 * for an example of how a BodyParser can be tested.
 */
class JsonUnitLinkEditBodyParserSpec extends FreeSpec with Matchers with GuiceOneAppPerSuite with ScalaFutures with EitherValues {

  private trait Fixture extends StubPlayBodyParsersFactory {
    val FromParentLEU = "123456789"
    val ToParentLEU = "234567890"
    val Username = "abcd"
    val VATEditParentLinkPostBody =
      s"""{
          |  "parent": {
          |    "from": {
          |      "id": "$FromParentLEU",
          |      "type": "LEU"
          |    },
          |    "to": {
          |      "id": "$ToParentLEU",
          |      "type": "LEU"
          |    }
          |  },
          |  "audit": { "username": "$Username" }
          |}""".stripMargin
    val InvalidVATEditParentLinkPostBody = VATEditParentLinkPostBody + "}"
    val BeforeAfterUnprocessableVATEditParentLinkPostBody = VATEditParentLinkPostBody.replace("from", "before")
      .replace("to", "after")
    val NoIdUnprocessableVATEditParentLinkPostBody = VATEditParentLinkPostBody.replace(s"""\"id\": \"$FromParentLEU\",""", "")
    val NumberUnprocessableVATEditParentLinkPostBody = VATEditParentLinkPostBody.replace(s"""\"$FromParentLEU\"""", FromParentLEU)
    val UnitTypeUnprocessableVATEditParentLinkPostBody = VATEditParentLinkPostBody.replace("LEU", "Legal Unit")
    val from = IdAndType(UnitId(FromParentLEU), UnitType.LegalUnit)
    val to = IdAndType(UnitId(ToParentLEU), UnitType.LegalUnit)
    val parent = Parent(from, to)

    implicit val materializer = app.materializer
    val jsonUnitLinkEditBodyParser = new JsonUnitLinkEditBodyParser(stubPlayBodyParsers.json)(ExecutionContext.global)
  }

  "A body representing a JSON patch specification" - {
    "can be parsed when valid" in new Fixture {
      val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON)
      val body = Source.single(ByteString(VATEditParentLinkPostBody))

      whenReady(jsonUnitLinkEditBodyParser(request).run(body)) { result =>
        result.right.value shouldBe EditParentLink(parent, Map("username" -> Username))
      }
    }

    "is rejected" - {
      "when the media type is not that of Json" in new Fixture {
        val request = FakeRequest().withHeaders(CONTENT_TYPE -> XML)
        val body = Source.single(ByteString(VATEditParentLinkPostBody))

        whenReady(jsonUnitLinkEditBodyParser(request).run(body)) { result =>
          result.left.value.header.status shouldBe UNSUPPORTED_MEDIA_TYPE
        }
      }

      "when the patch document is not valid json" in new Fixture {
        val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON)
        val body = Source.single(ByteString(InvalidVATEditParentLinkPostBody))

        whenReady(jsonUnitLinkEditBodyParser(request).run(body)) { result =>
          result.left.value.header.status shouldBe BAD_REQUEST
        }
      }

      "when the patch document valid json but does not comply with our model" - {
        "use of before/after rather than from/to" in new Fixture {
          val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON)
          val body = Source.single(ByteString(BeforeAfterUnprocessableVATEditParentLinkPostBody))

          whenReady(jsonUnitLinkEditBodyParser(request).run(body)) { result =>
            result.left.value.header.status shouldBe UNPROCESSABLE_ENTITY
          }
        }

        "from map does not contain an id" in new Fixture {
          val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON)
          val body = Source.single(ByteString(NoIdUnprocessableVATEditParentLinkPostBody))

          whenReady(jsonUnitLinkEditBodyParser(request).run(body)) { result =>
            result.left.value.header.status shouldBe UNPROCESSABLE_ENTITY
          }
        }

        "from id contains a number rather than a string" in new Fixture {
          val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON)
          val body = Source.single(ByteString(NumberUnprocessableVATEditParentLinkPostBody))

          whenReady(jsonUnitLinkEditBodyParser(request).run(body)) { result =>
            result.left.value.header.status shouldBe UNPROCESSABLE_ENTITY
          }
        }

        "invalid unit type" in new Fixture {
          val request = FakeRequest().withHeaders(CONTENT_TYPE -> JSON)
          val body = Source.single(ByteString(UnitTypeUnprocessableVATEditParentLinkPostBody))

          whenReady(jsonUnitLinkEditBodyParser(request).run(body)) { result =>
            result.left.value.header.status shouldBe UNPROCESSABLE_ENTITY
          }
        }
      }
    }
  }
}