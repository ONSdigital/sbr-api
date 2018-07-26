package support.tracing

import actions.TracedRequest
import brave.propagation.TraceContext
import brave.{ Span, Tracing }
import jp.co.bizreach.trace.ZipkinTraceServiceLike
import org.scalatest.matchers.{ BeMatcher, MatchResult }

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

  protected def fakeSpan(traceIdHigh: Long, traceIdLow: Long, parentSpanId: Long, spanId: Long): Span =
    fakeSpan(TraceContext.newBuilder().
      traceIdHigh(traceIdHigh).
      traceId(traceIdLow).
      parentId(parentSpanId).
      spanId(spanId).
      build())

  protected def fakeSpan(traceContext: TraceContext): Span =
    Tracing.newBuilder().build().tracer().toSpan(traceContext)

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

  object TraceContextMatcher {
    private class TraceContextMatcher(traceIdHigh: Long, traceIdLow: Long, spanId: Long, parentSpanId: Option[Long]) extends BeMatcher[TraceContext] {
      override def apply(actual: TraceContext): MatchResult =
        MatchResult(
          isMatch(actual),
          failureMessage(actual),
          negatedFailureMessage
        )

      private def isMatch(actual: TraceContext): Boolean =
        actual.traceIdHigh() == traceIdHigh &&
          actual.traceId() == traceIdLow &&
          actual.spanId() == spanId &&
          isMatchOfParentSpanId(actual.parentId())

      private def isMatchOfParentSpanId(actualParentSpanId: java.lang.Long): Boolean =
        parentSpanId.fold(actualParentSpanId == null) { id =>
          Long.box(id) == actualParentSpanId
        }

      private def failureMessage(actual: TraceContext): String =
        s"TraceContext with traceIdHigh=[${actual.traceIdHigh().toHexString}] " +
          s"traceIdLow=[${actual.traceId().toHexString}] " +
          s"parentSpanId=[${nullableValue(actual.parentId())}] " +
          s"spanId=[${actual.spanId()}] did not match TraceContext with " +
          s"traceIdHigh=[${traceIdHigh.toHexString}] " +
          s"traceIdLow=[${traceIdLow.toHexString}] " +
          s"parentSpanId=[${parentSpanId.fold("null")(_.toHexString)}] " +
          s"spanId=[${spanId.toHexString}]"

      private def nullableValue(value: java.lang.Long): String =
        if (value == null) "null" else value.toLong.toHexString

      private def negatedFailureMessage: String =
        s"TraceContext matched withTraceIdHigh = [${traceIdHigh.toHexString}] " +
          s"traceIdLow=[${traceIdLow.toHexString}] " +
          s"parentSpanId=[${parentSpanId.fold("null")(_.toHexString)}] " +
          s"spanId=[${spanId.toHexString}]"
    }

    def aTraceDataContext(withTraceIdHigh: Long, withTraceIdLow: Long, withSpanId: Long, withParentSpanId: Option[Long] = None): BeMatcher[TraceContext] =
      new TraceContextMatcher(withTraceIdHigh, withTraceIdLow, withSpanId, withParentSpanId)
  }
}
