package tracing

import brave.propagation.TraceContext
import org.scalatest.{ FreeSpec, Matchers }
import org.slf4j.MDC
import support.tracing.FakeTracing
import tracing.TracingDiagnosticContext.ContextKeys.{ ParentSpanId, SpanId, TraceId }

class TracingDiagnosticContextSpec extends FreeSpec with Matchers with FakeTracing {

  private trait Fixture {
    val TraceIdHighValue = 0xdf9cd2d3c54bd28dL
    val TraceIdLowValue = 0xeb9582f6b2b6dc35L
    val ParentSpanIdValue = 0xfad55fdd469a9d86L
    val SpanIdValue = 0x07629e929e9c3d85L

    def fakeTraceContext: TraceContext =
      fakeSpan(TraceIdHighValue, TraceIdLowValue, ParentSpanIdValue, SpanIdValue).context()
  }

  "A TracingDiagnosticContext" - {
    "can be populated with the context of a trace" - {
      "when a child span (with parent spanId)" in new Fixture {
        MDC.clear()

        TracingDiagnosticContext.setContext(fakeTraceContext)

        MDC.get(TraceId) shouldBe TraceIdHighValue.toHexString + TraceIdLowValue.toHexString
        MDC.get(ParentSpanId) shouldBe ParentSpanIdValue.toHexString
        MDC.get(SpanId) shouldBe SpanIdValue.toHexString
      }

      "when a root span (with no parent spanId)" in new Fixture {
        MDC.clear()
        val traceContext = fakeTraceContext.toBuilder.parentId(null).build()

        TracingDiagnosticContext.setContext(traceContext)

        MDC.get(TraceId) shouldBe TraceIdHighValue.toHexString + TraceIdLowValue.toHexString
        MDC.get(ParentSpanId) shouldBe "null"
        MDC.get(SpanId) shouldBe SpanIdValue.toHexString
      }
    }
  }
}
