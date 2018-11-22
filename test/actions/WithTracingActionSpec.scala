package actions

import jp.co.bizreach.trace.ZipkinTraceServiceLike
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, Matchers}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContent, Result}
import play.api.test.{FakeRequest, StubPlayBodyParsersFactory}
import support.tracing.FakeTracing

import scala.concurrent.{ExecutionContext, Future}

class WithTracingActionSpec extends FreeSpec with Matchers with MockFactory with ScalaFutures with FakeTracing with GuiceOneAppPerSuite {

  private trait Fixture extends StubPlayBodyParsersFactory {
    val TraceIdHigh = 0xb787a0cecda09171L
    val TraceIdLow = 0x694e46a737dc1097L
    val ParentSpanId = 0xa642df366ccebdb8L
    val SpanId = 0x2b5a2554fdf088f2L

    implicit val materializer = app.materializer
    val block = mockFunction[TracedRequest[AnyContent], Future[Result]]
    val traceService: ZipkinTraceServiceLike = new StubTraceService(fakeSpan(TraceIdHigh, TraceIdLow, ParentSpanId, SpanId))
    val tracingAction = new WithTracingAction(stubPlayBodyParsers.default, traceService)(ExecutionContext.global)
  }

  "WithTracingAction" - {
    "adds TraceData to the request" in new Fixture {
      val request = FakeRequest("GET", "/some-uri")

      block.expects(where(aRequestWithTraceData[AnyContent](
        withTraceIdHigh = TraceIdHigh,
        withTraceIdLow = TraceIdLow,
        withParentSpanId = ParentSpanId,
        withSpanId = SpanId
      ))).returns(Future.successful(Ok))

      whenReady(tracingAction.invokeBlock(request, block)) { result =>
        result shouldBe Ok
      }
    }
  }
}
