package repository.sbrctrl

import com.github.tomakehurst.wiremock.client.WireMock.{ aResponse, get, urlEqualTo }
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Second, Span }
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{ BAD_REQUEST, SERVICE_UNAVAILABLE }
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import support.WithWireMockSbrControlApi
import utils.url.BaseUrl
import utils.url.BaseUrl.Protocol.Http

class SbrCtrlUnitRepository_WiremockSpec extends org.scalatest.fixture.FreeSpec with WithWireMockSbrControlApi with Matchers with ScalaFutures with EitherValues with GuiceOneAppPerSuite with MockFactory {

  private val SomePath = "foo/bar/baz"
  private val SomeJsonStr = s"""{"message":"Hello World!"}"""
  private val RequestTimeoutMillis = 100

  // artificially reduce the default request timeout for the purposes of testing timeout handling.
  override def fakeApplication(): Application = new GuiceApplicationBuilder().configure("play.ws.timeout.request" -> RequestTimeoutMillis).build()

  // test patience must exceed the configured fixedDelay to properly test client-side timeout handling
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = scaled(Span(1, Second)), interval = scaled(Span(100, Millis)))

  protected case class FixtureParam(repository: SbrCtrlUnitRepository)

  /*
   * Note that we cannot use WsTestClient here as it does not respect the request timeout settings of the app,
   * and we want to test timeout handling behaviour.
   */
  override protected def withFixture(test: OneArgTest): Outcome = {
    val config = SbrCtrlUnitRepositoryConfig(BaseUrl(Http, "localhost", wireMockPort))
    val wsClient = app.injector.instanceOf[WSClient]
    withFixture(test.toNoArgTest(FixtureParam(new SbrCtrlUnitRepository(config, wsClient))))
  }

  "A unit repository" - {
    "when requested to retrieve json for a resource" - {
      "returns json when the resource is found" in { fixture =>
        stubSbrControlApiFor(get(urlEqualTo(s"""/$SomePath""")).willReturn(anOkResponse().withBody(SomeJsonStr)))

        whenReady(fixture.repository.getJson(SomePath)) { result =>
          result.right.value shouldBe Some(Json.parse(SomeJsonStr))
        }
      }

      "returns nothing when the resource is not found" in { fixture =>
        stubSbrControlApiFor(get(urlEqualTo(s"""/$SomePath""")).willReturn(aNotFoundResponse()))

        whenReady(fixture.repository.getJson(SomePath)) { result =>
          result.right.value shouldBe empty
        }
      }
    }

    "fails" - {
      "when the response is a client error" in { fixture =>
        stubSbrControlApiFor(get(urlEqualTo(s"""/$SomePath""")).willReturn(aResponse().withStatus(BAD_REQUEST)))

        whenReady(fixture.repository.getJson(SomePath)) { result =>
          result.left.value shouldBe "Bad Request (400)"
        }
      }

      "when the response is a server error" in { fixture =>
        stubSbrControlApiFor(get(urlEqualTo(s"""/$SomePath""")).willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE)))

        whenReady(fixture.repository.getJson(SomePath)) { result =>
          result.left.value shouldBe "Service Unavailable (503)"
        }
      }

      "when an OK response is returned containing a non-JSON body" in { fixture =>
        stubSbrControlApiFor(get(urlEqualTo(s"""/$SomePath""")).willReturn(anOkResponse().withBody("this-is-not-json")))

        whenReady(fixture.repository.getJson(SomePath)) { result =>
          result.left.value should startWith("Unable to create JsValue from sbr-control-api response")
        }
      }

      /*
       * Test patienceConfig must exceed the fixedDelay for this to work ...
       */
      "when the server takes longer than the configured client-side timeout" in { fixture =>
        stubSbrControlApiFor(get(urlEqualTo(s"""/$SomePath""")).willReturn(anOkResponse().withBody(SomeJsonStr).
          withFixedDelay(RequestTimeoutMillis * 2)))

        whenReady(fixture.repository.getJson(SomePath)) { result =>
          result.left.value should startWith("Timeout")
        }
      }
    }
  }
}
