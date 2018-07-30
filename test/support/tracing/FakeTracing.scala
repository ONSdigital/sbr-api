package support.tracing

import actions.TracedRequest
import brave.propagation.TraceContext
import brave.{ Span, Tracing }
import jp.co.bizreach.trace.ZipkinTraceServiceLike

import scala.concurrent.ExecutionContext

trait FakeTracing {
  /*
   * We cannot use scalamock for this as the key method we would need to mock/stub (toSpan) is not accessible.
   * We therefore use a hand-cranked stub.
   */
  protected class StubTraceService(span: Span) extends ZipkinTraceServiceLike {
    override implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    override val tracing: Tracing = Tracing.newBuilder().build()

    override def toSpan[A](headers: A)(getHeader: (A, String) => Option[String]): Span =
      span
  }

  protected def fakeSpan(traceIdHigh: Long, traceIdLow: Long, parentSpanId: Long, spanId: Long): Span = {
    val traceContext = TraceContext.newBuilder().
      traceIdHigh(traceIdHigh).
      traceId(traceIdLow).
      parentId(parentSpanId).
      spanId(spanId).
      build()
    Tracing.newBuilder().build().tracer().toSpan(traceContext)
  }

  def aTraceContext(withTraceIdHigh: Long, withTraceIdLow: Long, withParentSpanId: Long, withSpanId: Long)(actual: TraceContext): Boolean = {
    val result = actual.traceIdHigh() == withTraceIdHigh &&
      actual.traceId() == withTraceIdLow &&
      actual.parentId() == withParentSpanId &&
      actual.spanId() == withSpanId

    if (!result) {
      val expected = TraceContext.newBuilder().
        traceIdHigh(withTraceIdHigh).
        traceId(withTraceIdLow).
        parentId(withParentSpanId).
        spanId(withSpanId).
        build()
      System.err.println(s"Expected TraceContext of [$expected] but was [$actual].")
    }
    result
  }

  def aRequestWithTraceData[A](withTraceIdHigh: Long, withTraceIdLow: Long, withParentSpanId: Long, withSpanId: Long): TracedRequest[A] => Boolean =
    (tracedRequest: TracedRequest[A]) =>
      aTraceContext(withTraceIdHigh, withTraceIdLow, withParentSpanId, withSpanId)(tracedRequest.traceData.asSpan.context())
}
