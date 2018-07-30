package actions

import brave.Span
import javax.inject.Inject
import jp.co.bizreach.trace.ZipkinTraceServiceLike
import jp.co.bizreach.trace.play25.implicits.ZipkinTraceImplicits
import play.api.mvc.{ ActionBuilder, ActionTransformer, Request, WrappedRequest }
import tracing.TraceData

import scala.concurrent.Future

class TracedRequest[A](val traceData: TraceData, originalRequest: Request[A]) extends WrappedRequest[A](originalRequest)

class WithTracingAction @Inject() (val tracer: ZipkinTraceServiceLike) extends ActionBuilder[TracedRequest] with ActionTransformer[Request, TracedRequest] with ZipkinTraceImplicits {
  override protected def transform[A](request: Request[A]): Future[TracedRequest[A]] =
    Future.successful(newRequestWithTraceData(request))

  private def newRequestWithTraceData[A](request: Request[A]): TracedRequest[A] = {
    val span = request2trace(request).span
    val traceData = new TraceData {
      override def asSpan: Span =
        span
    }
    new TracedRequest[A](traceData, request)
  }
}
