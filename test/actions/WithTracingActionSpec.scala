package actions

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ EitherValues, FreeSpec, Matchers }
import org.slf4j.MDC
import play.api.mvc.Results.Ok
import play.api.mvc.{ AnyContent, Result }
import play.api.test.FakeRequest
import support.tracing.FakeTracing
import tracing.TracingDiagnosticContext.ContextKeys._

import scala.concurrent.Future

class WithTracingActionSpec extends FreeSpec with Matchers with MockFactory with ScalaFutures with FakeTracing with EitherValues {

  import TraceContextMatcher.aTraceDataContext

  private trait Fixture {
    val TraceIdHighValue = 0xb787a0cecda09171L
    val TraceIdLowValue = 0x694e46a737dc1097L
    val ParentSpanIdValue = 0xa642df366ccebdb8L
    val SpanIdValue = 0x2b5a2554fdf088f2L
    val span = fakeSpan(TraceIdHighValue, TraceIdLowValue, ParentSpanIdValue, SpanIdValue)

    val block = mockFunction[TracedRequest[AnyContent], Future[Result]]

    def assertTracingDiagnosticContextContains(traceId: String, parentSpanId: String, spanId: String): Unit = {
      withClue("traceId") {
        MDC.get(TraceId) shouldBe traceId
      }

      withClue("parentSpanId") {
        MDC.get(ParentSpanId) shouldBe parentSpanId
      }

      withClue("spanId") {
        MDC.get(SpanId) shouldBe spanId
      }

      ()
    }
  }

  "WithTracingAction" - {
    "adds TraceData to the request" in new Fixture {
      val request = FakeRequest("GET", "/some-uri")
      val traceService = new StubTraceService(span)
      val tracingAction = new WithTracingAction(traceService)

      whenReady(tracingAction.refine[AnyContent](request)) { result =>
        val tracedRequest = result.right.value

        tracedRequest.traceData.asSpan.context() shouldBe aTraceDataContext(
          withTraceIdHigh = TraceIdHighValue,
          withTraceIdLow = TraceIdLowValue,
          withParentSpanId = Some(ParentSpanIdValue),
          withSpanId = SpanIdValue
        )
      }
    }

    "invokes the controller block with a request that has TraceData" in new Fixture {
      val request = FakeRequest("GET", "/some-uri")
      val traceService = new StubTraceService(span)
      val tracingAction = new WithTracingAction(traceService)
      block.expects(where(aRequestWithTraceData[AnyContent](
        withTraceIdHigh = TraceIdHighValue,
        withTraceIdLow = TraceIdLowValue,
        withParentSpanId = ParentSpanIdValue,
        withSpanId = SpanIdValue
      ))).returns(Future.successful(Ok))

      whenReady(tracingAction.invokeBlock(request, block)) { result =>
        result shouldBe Ok
      }
    }

    "adds TraceData to the Mapped Diagnostic Context" - {
      "when the request is a child span (with parent spanId)" in new Fixture {
        val request = FakeRequest("GET", "/some-uri")
        val traceService = new StubTraceService(span)
        val tracingAction = new WithTracingAction(traceService)

        whenReady(tracingAction.refine(request)) { _ =>
          assertTracingDiagnosticContextContains(
            traceId = TraceIdHighValue.toHexString + TraceIdLowValue.toHexString,
            parentSpanId = ParentSpanIdValue.toHexString,
            spanId = SpanIdValue.toHexString
          )
        }
      }

      "when the request is a root span (no parent spanId)" in new Fixture {
        val request = FakeRequest("GET", "/some-uri")
        val rootSpan = fakeSpan(span.context().toBuilder.parentId(null).build())
        val traceService = new StubTraceService(rootSpan)
        val tracingAction = new WithTracingAction(traceService)

        whenReady(tracingAction.refine(request)) { _ =>
          assertTracingDiagnosticContextContains(
            traceId = TraceIdHighValue.toHexString + TraceIdLowValue.toHexString,
            parentSpanId = "null",
            spanId = SpanIdValue.toHexString
          )
        }
      }
    }
  }
}
