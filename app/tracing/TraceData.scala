package tracing

import brave.Span

trait TraceData {
  def asSpan: Span
}