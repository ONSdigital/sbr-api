package tracing

import brave.Tracing
import brave.propagation.TraceContext
import jp.co.bizreach.trace.ZipkinTraceServiceLike
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FreeSpec, Matchers }
import play.api.libs.ws.{ WSClient, WSRequest }

class ZipkinTraceWSClientSpec extends FreeSpec with Matchers with MockFactory {

  private trait Fixture {
    val SomeUrl = "/some-url"

    val wsClient = mock[WSClient]
    val zipkinTraceServiceLike = stub[ZipkinTraceServiceLike]
    val zipkinTraceWSClient = new jp.co.bizreach.trace.play26.TraceWSClient(wsClient, zipkinTraceServiceLike)

    val wsRequest = stub[WSRequest]
    val traceData = stub[TraceData]
    val fakeTraceContext = TraceContext.newBuilder().traceId(0x7b1fe14fb2c02751L).spanId(0xd1409af47eb8bac8L).build()
    (traceData.asSpan _).when().returns(Tracing.newBuilder().build().tracer().joinSpan(fakeTraceContext))

    val traceWSClient = new ZipkinTraceWSClient(zipkinTraceWSClient)
  }

  "A ZipkinTraceWSClient" - {
    "delegates to the underlying wsClient" - {
      "url" in new Fixture {
        (wsClient.url _).expects(SomeUrl).returning(wsRequest)

        traceWSClient.url(SomeUrl, "some-span-name", traceData)
      }

      "close" in new Fixture {
        (wsClient.close _).expects()

        traceWSClient.close()
      }
    }
  }
}
