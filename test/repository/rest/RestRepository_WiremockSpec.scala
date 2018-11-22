package repository.rest

import brave.propagation.TraceContext
import com.github.tomakehurst.wiremock.client.WireMock._
import jp.co.bizreach.trace.ZipkinTraceServiceLike
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Second, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsers.JsonUnitLinkEditBodyParser.JsonPatchMediaType
import play.api.Application
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.Status.{BAD_REQUEST, SERVICE_UNAVAILABLE}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsString, Json}
import support.wiremock.WireMockSbrControlApi
import tracing.{TraceData, TraceWSClient}
import uk.gov.ons.sbr.models.edit._
import uk.gov.ons.sbr.models.{Period, VatRef}
import utils.url.BaseUrl
import utils.url.BaseUrl.Protocol.Http

import scala.concurrent.ExecutionContext

class RestRepository_WiremockSpec extends org.scalatest.fixture.FreeSpec with GuiceOneAppPerSuite with WireMockSbrControlApi with Matchers with ScalaFutures with EitherValues with MockFactory {

  private val SomePath = "foo/bar/baz"
  private val SomeSpanName = "span-name"
  private val SomeTraceData = stub[TraceData]
  private val SomeJsonStr = s"""{"message":"Hello World!"}"""
  private val RequestTimeoutMillis = 100

  private val TargetVAT = VatRef("123456789012")
  private val TargetPeriod = Period.fromString("201803")
  private val TargetFromUBRN = "123456789"
  private val TargetToUBRN = "987654321"

  private val TestReplacePatch: Patch = Seq(
    TestOperation(Path("/parents/", "LEU"), JsString(TargetFromUBRN)),
    ReplaceOperation(Path("/parents/", "LEU"), JsString(TargetToUBRN))
  )

  private val VATEditParentLinkPatchBody =
    s"""|[
        |  { "op": "test", path: "/parents/LEU", value: "123456789" },
        |  { "op": "replace", path: "/parents/LEU", value: "987654321" }
        |]""".stripMargin

  /*
   * Turn off auto-verification of mocks inherited from AbstractMockFactory.
   *
   * With auto-verification enabled this test will fail with:
   *   assertion failed: Null expectation context - missing withExpectations?
   *   java.lang.AssertionError: assertion failed: Null expectation context - missing withExpectations?
   * possibly as a result of the order of operations.
   *
   * Tracing is not part of this test, and is only involved because we have to pass TraceData to the system under test.
   * Verification is not therefore required.
   */
  autoVerify = false

  // artificially reduce the default request timeout for the purposes of testing timeout handling.
  override def fakeApplication(): Application = new GuiceApplicationBuilder().configure("play.ws.timeout.request" -> RequestTimeoutMillis).build()

  // test patience must exceed the configured fixedDelay to properly test client-side timeout handling
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = scaled(Span(1, Second)), interval = scaled(Span(100, Millis)))

  protected case class FixtureParam(repository: RestRepository)

  override protected def withFixture(test: OneArgTest): Outcome =
    withWireMockSbrControlApi { () =>
      withFixture {
        (SomeTraceData.asSpan _).when().returns(fakeSpan)
        test.toNoArgTest(newFixtureParam)
      }
    }

  private def fakeSpan: brave.Span = {
    val traceContext = TraceContext.newBuilder().traceId(0x54c3bbc70482705fL).spanId(0xc17a932c627ba20bL).build()
    val traceService = app.injector.instanceOf[ZipkinTraceServiceLike]
    traceService.tracing.tracer().joinSpan(traceContext)
  }

  private def newFixtureParam: FixtureParam = {
    val config = RestRepositoryConfig(BaseUrl(Http, "localhost", DefaultSbrControlApiPort))
    val wsClient = app.injector.instanceOf[TraceWSClient]
    FixtureParam(new RestRepository(config, wsClient)(ExecutionContext.global))
  }

  "A unit repository" - {
    "when requested to retrieve json for a resource" - {
      "returns json when the resource is found" in { fixture =>
        stubSbrControlApiFor(get(urlEqualTo(s"""/$SomePath""")).willReturn(anOkResponse().withBody(SomeJsonStr)))

        whenReady(fixture.repository.getJson(SomePath, SomeSpanName, SomeTraceData)) { result =>
          result.right.value shouldBe Some(Json.parse(SomeJsonStr))
        }
      }

      "returns nothing when the resource is not found" in { fixture =>
        stubSbrControlApiFor(get(urlEqualTo(s"""/$SomePath""")).willReturn(aNotFoundResponse()))

        whenReady(fixture.repository.getJson(SomePath, SomeSpanName, SomeTraceData)) { result =>
          result.right.value shouldBe empty
        }
      }
    }

    "when requested to patch json for a resource" - {
      "returns PatchSuccess when the resource is found" in { fixture =>
        stubSbrControlApiFor(aVatParentLinkEditRequest(withVatRef = TargetVAT, withPeriod = TargetPeriod)
          .withHeader(CONTENT_TYPE, equalTo(JsonPatchMediaType))
          .withRequestBody(equalToJson(VATEditParentLinkPatchBody))
          .willReturn(aNoContentResponse()))

        whenReady(fixture.repository.patchJson(s"v1/periods/${Period.asString(TargetPeriod)}/types/VAT/units/${TargetVAT.value}", TestReplacePatch)) { result =>
          result shouldBe PatchSuccess
        }
      }

      "returns PatchUnitNotFound when the resource is not found" in { fixture =>
        stubSbrControlApiFor(get(urlEqualTo(s"""/$SomePath""")).willReturn(aNotFoundResponse()))

        whenReady(fixture.repository.patchJson(SomePath, TestReplacePatch)) { result =>
          result shouldBe PatchUnitNotFound
        }
      }
    }

    "fails" - {
      "when requested to get json for a resource" - {
        "when the response is a client error" in { fixture =>
          stubSbrControlApiFor(get(urlEqualTo(s"""/$SomePath""")).willReturn(aResponse().withStatus(BAD_REQUEST)))

          whenReady(fixture.repository.getJson(SomePath, SomeSpanName, SomeTraceData)) { result =>
            result.left.value shouldBe "Bad Request (400)"
          }
        }

        "when the response is a server error" in { fixture =>
          stubSbrControlApiFor(get(urlEqualTo(s"""/$SomePath""")).willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE)))

          whenReady(fixture.repository.getJson(SomePath, SomeSpanName, SomeTraceData)) { result =>
            result.left.value shouldBe "Service Unavailable (503)"
          }
        }

        "when an OK response is returned containing a non-JSON body" in { fixture =>
          stubSbrControlApiFor(get(urlEqualTo(s"""/$SomePath""")).willReturn(anOkResponse().withBody("this-is-not-json")))

          whenReady(fixture.repository.getJson(SomePath, SomeSpanName, SomeTraceData)) { result =>
            result.left.value should startWith("Unable to create JsValue from unit response")
          }
        }

        /*
       * Test patienceConfig must exceed the fixedDelay for this to work ...
       */
        "when the server takes longer than the configured client-side timeout" in { fixture =>
          stubSbrControlApiFor(get(urlEqualTo(s"""/$SomePath""")).willReturn(anOkResponse().withBody(SomeJsonStr).
            withFixedDelay(RequestTimeoutMillis * 2)))

          whenReady(fixture.repository.getJson(SomePath, SomeSpanName, SomeTraceData)) { result =>
            result.left.value should startWith("Timeout")
          }
        }
      }

      "when requested to patch json for a resource" - {
        "when the response is a server error" in { fixture =>
          stubSbrControlApiFor(aVatParentLinkEditRequest(withVatRef = TargetVAT, withPeriod = TargetPeriod)
            .withHeader(CONTENT_TYPE, equalTo(JsonPatchMediaType))
            .withRequestBody(equalToJson(VATEditParentLinkPatchBody))
            .willReturn(anInternalServerError()))

          whenReady(fixture.repository.patchJson(s"v1/periods/${Period.asString(TargetPeriod)}/types/VAT/units/${TargetVAT.value}", TestReplacePatch)) { result =>
            result shouldBe PatchFailure
          }
        }

        "when the patch request is rejected" in { fixture =>
          stubSbrControlApiFor(aVatParentLinkEditRequest(withVatRef = TargetVAT, withPeriod = TargetPeriod)
            .withHeader(CONTENT_TYPE, equalTo(JsonPatchMediaType))
            .withRequestBody(equalToJson(VATEditParentLinkPatchBody))
            .willReturn(anUnprocessableEntityResponse()))

          whenReady(fixture.repository.patchJson(s"v1/periods/${Period.asString(TargetPeriod)}/types/VAT/units/${TargetVAT.value}", TestReplacePatch)) { result =>
            result shouldBe PatchRejected
          }
        }

        "when the response is conflict" in { fixture =>
          stubSbrControlApiFor(aVatParentLinkEditRequest(withVatRef = TargetVAT, withPeriod = TargetPeriod)
            .withHeader(CONTENT_TYPE, equalTo(JsonPatchMediaType))
            .withRequestBody(equalToJson(VATEditParentLinkPatchBody))
            .willReturn(aConflictResponse()))

          whenReady(fixture.repository.patchJson(s"v1/periods/${Period.asString(TargetPeriod)}/types/VAT/units/${TargetVAT.value}", TestReplacePatch)) { result =>
            result shouldBe PatchConflict
          }
        }

        /*
         * Test patienceConfig must exceed the fixedDelay for this to work ...
         */
        "when the server takes longer than the configured client-side timeout" in { fixture =>
          stubSbrControlApiFor(aVatParentLinkEditRequest(withVatRef = TargetVAT, withPeriod = TargetPeriod)
            .withHeader(CONTENT_TYPE, equalTo(JsonPatchMediaType))
            .withRequestBody(equalToJson(VATEditParentLinkPatchBody))
            .willReturn(aConflictResponse().withFixedDelay(RequestTimeoutMillis * 2)))

          whenReady(fixture.repository.patchJson(s"v1/periods/${Period.asString(TargetPeriod)}/types/VAT/units/${TargetVAT.value}", TestReplacePatch)) { result =>
            result shouldBe PatchFailure
          }
        }
      }
    }
  }
}