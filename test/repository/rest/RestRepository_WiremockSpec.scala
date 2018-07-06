package repository.rest

import brave.propagation.TraceContext
import com.github.tomakehurst.wiremock.client.WireMock.{ aResponse, get, urlEqualTo }
import jp.co.bizreach.trace.ZipkinTraceServiceLike
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Second, Span }
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{ BAD_REQUEST, SERVICE_UNAVAILABLE }
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import support.wiremock.WireMockSbrControlApi
import tracing.{ TraceData, TraceWSClient }
import utils.url.BaseUrl
import utils.url.BaseUrl.Protocol.Http

class RestRepository_WiremockSpec extends org.scalatest.fixture.FreeSpec with GuiceOneAppPerSuite with WireMockSbrControlApi with Matchers with ScalaFutures with EitherValues with MockFactory {

  private val SomePath = "foo/bar/baz"
  private val SomeSpanName = "span-name"
  private val SomeTraceData = stub[TraceData]
  private val SomeJsonStr = s"""{"message":"Hello World!"}"""
  private val RequestTimeoutMillis = 100

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
    FixtureParam(new RestRepository(config, wsClient))
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

    "fails" - {
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
  }
}
