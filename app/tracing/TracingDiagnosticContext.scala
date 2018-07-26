package tracing

import brave.propagation.TraceContext
import org.slf4j.MDC
import tracing.TracingDiagnosticContext.ContextKeys.{ ParentSpanId, SpanId, TraceId }

object TracingDiagnosticContext {
  object ContextKeys {
    val TraceId = "TraceId"
    val ParentSpanId = "ParentSpanId"
    val SpanId = "SpanId"
  }

  def setContext(traceContext: TraceContext): Unit = {
    MDC.put(TraceId, traceContext.traceIdString())
    MDC.put(ParentSpanId, nullableValue(traceContext.parentId()))
    MDC.put(SpanId, traceContext.spanId().toHexString)
  }

  private def nullableValue(value: Number): String =
    if (value == null) "null"
    else value.longValue().toHexString
}
