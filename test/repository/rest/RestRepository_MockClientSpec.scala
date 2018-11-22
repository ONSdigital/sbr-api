package repository.rest

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{EitherValues, FreeSpec, Matchers}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import tracing.{TraceData, TraceWSClient}
import utils.url.BaseUrl
import utils.url.BaseUrl.Protocol.Http

import scala.concurrent.{ExecutionContext, Future}

/*
 * This spec mocks the wsClient, which disregards the rule "don't mock types you don't own" (see "Growing
 * Object-Oriented Software, Guided by Tests" by Freeman & Pryce).  Prefer the sibling test that uses Wiremock
 * where possible.  This was introduced in order to assert that the configured baseUrl is used, as the
 * wsTestClient used by the acceptance test overrides the host / port.
 */
class RestRepository_MockClientSpec extends FreeSpec with Matchers with MockFactory with ScalaFutures with EitherValues {

  private trait Fixture {
    val SomeRelativePath = "foo/bar/baz"
    val SomeSpanName = "span-name"

    val ws = stub[WSClient]
    val wsClient = stub[TraceWSClient]
    val traceData = stub[TraceData]
    val wsRequest = mock[WSRequest]
    val config = RestRepositoryConfig(
      BaseUrl(protocol = Http, host = "somehost", port = 4321)
    )
    val repository = new RestRepository(config, wsClient)(ExecutionContext.global)

    (wsRequest.withHttpHeaders _).expects(*).returning(wsRequest)
  }

  "A SBR Control unit repository" - {
    "targets the specified host and port when making a request" in new Fixture {
      (wsClient.url _).when(s"http://somehost:4321/$SomeRelativePath", SomeSpanName, traceData).returning(wsRequest)
      (wsRequest.get _).expects().returning(Future.successful(stub[WSResponse]))

      repository.getJson(SomeRelativePath, SomeSpanName, traceData)
    }

    /*
     * Any connection failed / socket disconnected type issue will likely result in the WsRequest's
     * Future failing.  This tests the "catch-all" case, and that we can effectively recover the Future.
     */
    "materialises a failure into an error message" in new Fixture {
      (wsClient.url _).when(*, SomeSpanName, traceData).returns(wsRequest)
      (wsRequest.get _).expects().returning(Future.failed(new Exception("Connection failed")))

      whenReady(repository.getJson(SomeRelativePath, SomeSpanName, traceData)) { result =>
        result.left.value shouldBe "Connection failed"
      }
    }
  }
}
