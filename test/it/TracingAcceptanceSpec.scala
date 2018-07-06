
import java.time.Month.JUNE

import TracingAcceptanceSpec.SpanMatcher.{ aChildSpan, aRootSpan }
import TracingAcceptanceSpec._
import com.google.inject.{ AbstractModule, TypeLiteral }
import com.typesafe.scalalogging.LazyLogging
import fixture.ServerAcceptanceSpec
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Seconds }
import org.scalatest.{ Outcome, TestData }
import play.api.Application
import play.api.http.Port
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.WsTestClient
import support.wiremock.WireMockSbrControlApi
import tracing.SpanNameCleaningReporter
import uk.gov.ons.sbr.models.{ Ern, Period }
import zipkin.Span
import zipkin.reporter.Reporter

import scala.collection._

class TracingAcceptanceSpec extends ServerAcceptanceSpec with MockFactory with WireMockSbrControlApi with ScalaFutures {

  // We want a fresh reporter per test, but need to retain a handle on the instance for assertion purposes.
  private var traceReporter: Reporter[Span] = _
  private val reportedSpans: mutable.ListBuffer[Span] = mutable.ListBuffer.empty

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(org.scalatest.time.Span(10, Seconds)),
    interval = scaled(org.scalatest.time.Span(500, Millis))
  )

  override def newAppForTest(testData: TestData): Application = {
    /*
     * We actually need a "spy", because when a root traceId is auto-allocated we need to capture the value
     * so that we can assert that child spans share the traceId.
     * ScalaMock does not provide "spy" support, so we emulate it by capturing all reported spans in our own
     * mutable collection.
     */
    reportedSpans.clear()
    traceReporter = stub[Reporter[Span]]
    (traceReporter.report _).when(*).onCall { (span: Span) =>
      reportedSpans += span
      ()
    }

    val fakeTracingModule = new AbstractModule {
      override def configure(): Unit = {
        bind(new TypeLiteral[Reporter[Span]]() {}).toInstance(new DebugReporter(new SpanNameCleaningReporter(traceReporter)))
      }
    }

    new GuiceApplicationBuilder().overrides(fakeTracingModule).build()
  }

  override type FixtureParam = WSClient

  override protected def withFixture(test: OneArgTest): Outcome =
    withWireMockSbrControlApi { () =>
      WsTestClient.withClient { wsClient =>
        withFixture(test.toNoArgTest(wsClient))
      }(new Port(port))
    }

  info("As a DevOps engineer")
  info("I want to be able to trace all calls made to sbr-api")
  info("So that I can debug and trace requests")

  feature("a trace is created for incoming requests that are missing a trace context") {
    scenario("when serving the request does not entail making downstream requests") { wsClient =>
      Given("the request will have no existing trace context")
      val timestampMicrosBeforeRequest = currentTimestampMicros

      When(s"a request is made for the sbr-api version")
      whenReady(wsClient.url("/version").get()) { _ =>

        Then(s"a root trace span is created to capture the request latency")
        (traceReporter.report _).verify(where(aRootSpan(
          withName = "get - /version",
          withTimestampMicrosAfter = timestampMicrosBeforeRequest
        )))

        And("no other spans are created")
        reportedSpans should have length 1
      }
    }

    scenario("when serving the request entails making downstream requests") { wsClient =>
      Given(s"a downstream unit link request will find the unit links")
      stubSbrControlApiFor(anEnterpriseUnitLinksRequest(withErn = TargetErn, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(unitLinksResponseBody(TargetErn, TargetPeriod))
      ))
      And(s"a downstream unit request will find the unit")
      stubSbrControlApiFor(anEnterpriseForPeriodRequest(withErn = TargetErn, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(unitResponseBody(TargetErn))
      ))
      And("the incoming request will have no existing trace context")
      val timestampMicrosBeforeRequest = currentTimestampMicros

      When(s"a request is made to retrieve a unit")
      whenReady(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/ents/${TargetErn.value}").get()) { _ =>

        Then(s"a root trace span is created to capture the overall request latency")
        (traceReporter.report _).verify(where(aRootSpan(
          withName = "get - /v1/periods/$period/ents/$ern",
          withTimestampMicrosAfter = timestampMicrosBeforeRequest
        )))
        val parentSpan = singletonSpanByName(reportedSpans.toList, name = "get - /v1/periods/$period/ents/$ern")

        And(s"a child trace span is created to capture the unit links request latency")
        (traceReporter.report _).verify(where(aChildSpan(
          withParentSpan = parentSpan,
          withName = "get-unit-links",
          withTimestampMicrosAfter = timestampMicrosBeforeRequest
        )))

        And(s"a child trace span is created to capture the unit request latency")
        (traceReporter.report _).verify(where(aChildSpan(
          withParentSpan = parentSpan,
          withName = "get-enterprise",
          withTimestampMicrosAfter = timestampMicrosBeforeRequest
        )))

        And("no other spans are created")
        reportedSpans should have length 3
      }
    }
  }

  feature("a span is created for incoming requests that have an existing trace context") {
    scenario("when serving the request does not entail making downstream requests") { wsClient =>
      Given("the request will have a trace context")
      val traceContext = makeTraceContext(RequestTraceIdHigh, RequestTraceIdLow, RequestSpanId)
      val timestampMicrosBeforeRequest = currentTimestampMicros

      When(s"a request is made for the sbr-api version")
      whenReady(wsClient.url("/version").withHeaders(traceContext: _*).get()) { _ =>

        Then(s"a child span is created within the existing trace to capture the request latency")
        (traceReporter.report _).verify(where(aChildSpan(
          withTraceIdHigh = RequestTraceIdHigh,
          withTraceIdLow = RequestTraceIdLow,
          withParentSpanId = RequestSpanId,
          withName = "get - /version",
          withTimestampMicrosAfter = timestampMicrosBeforeRequest
        )))

        And("no other spans are created")
        reportedSpans should have length 1
      }
    }

    scenario("when serving the request entails making downstream requests") { wsClient =>
      Given(s"a downstream unit link request will find the unit links")
      stubSbrControlApiFor(anEnterpriseUnitLinksRequest(withErn = TargetErn, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(unitLinksResponseBody(TargetErn, TargetPeriod))
      ))
      And(s"a downstream unit request will find the unit")
      stubSbrControlApiFor(anEnterpriseForPeriodRequest(withErn = TargetErn, withPeriod = TargetPeriod).willReturn(
        anOkResponse().withBody(unitResponseBody(TargetErn))
      ))
      And("the incoming request will have a trace context")
      val traceContext = makeTraceContext(RequestTraceIdHigh, RequestTraceIdLow, RequestSpanId)
      val timestampMicrosBeforeRequest = currentTimestampMicros

      When(s"a request is made to retrieve a unit")
      whenReady(wsClient.url(s"/v1/periods/${Period.asString(TargetPeriod)}/ents/${TargetErn.value}").withHeaders(traceContext: _*).get()) { _ =>

        Then(s"a child span is created within the existing trace to capture the request latency")
        (traceReporter.report _).verify(where(aChildSpan(
          withTraceIdHigh = RequestTraceIdHigh,
          withTraceIdLow = RequestTraceIdLow,
          withParentSpanId = RequestSpanId,
          withName = "get - /v1/periods/$period/ents/$ern",
          withTimestampMicrosAfter = timestampMicrosBeforeRequest
        )))
        val parentSpan = singletonSpanByName(reportedSpans.toList, name = "get - /v1/periods/$period/ents/$ern")

        And(s"a grandchild span is created within the existing trace to capture the unit links request latency")
        (traceReporter.report _).verify(where(aChildSpan(
          withParentSpan = parentSpan,
          withName = "get-unit-links",
          withTimestampMicrosAfter = timestampMicrosBeforeRequest
        )))

        And(s"a grandchild span is created within the existing trace to capture the unit request latency")
        (traceReporter.report _).verify(where(aChildSpan(
          withParentSpan = parentSpan,
          withName = "get-enterprise",
          withTimestampMicrosAfter = timestampMicrosBeforeRequest
        )))

        And("no other spans are created")
        reportedSpans should have length 3
      }
    }
  }
}

object TracingAcceptanceSpec {
  private val TargetErn = Ern("1234567890")
  private val TargetPeriod = Period.fromYearMonth(2018, JUNE)
  private val RequestTraceIdHigh = 0x36599d86b3de7f62L
  private val RequestTraceIdLow = 0x71ddb90fd26ac6a3L
  private val RequestSpanId = 0x09b5600fe57d0333L

  private def unitLinksResponseBody(ern: Ern, period: Period): String =
    s"""
       |{"id":"${ern.value}",
       | "children":{"10205415":"LEU","900000011":"LOU"},
       | "unitType":"ENT",
       | "period":"${Period.asString(period)}"
       |}""".stripMargin

  private def unitResponseBody(ern: Ern): String =
    s"""|{
        | "ern":"${ern.value}",
        | "entref":"some-entref",
        | "name":"some-name",
        | "postcode":"some-postcode",
        | "legalStatus":"some-legalStatus",
        | "employees":42
        |}""".stripMargin

  private def currentTimestampMicros: Long =
    System.currentTimeMillis() * 100L

  private def makeTraceContext(traceIdHigh: Long, traceIdLow: Long, spanId: Long): List[(String, String)] =
    List(
      ("X-B3-TraceId", traceIdHigh.toHexString + traceIdLow.toHexString),
      ("X-B3-SpanId", spanId.toHexString)
    )

  private def singletonSpanByName(spans: List[Span], name: String): Span = {
    val matchingSpans = spans.filter(_.name == name)
    if (matchingSpans.length != 1) throw new AssertionError(s"Expected a single span with name [$name] but there was [${matchingSpans.length}]")
    matchingSpans.head
  }

  object SpanMatcher {
    def aRootSpan(withName: String, withTimestampMicrosAfter: Long): Span => Boolean =
      (span: Span) =>
        span.parentId == null &&
          span.name == withName &&
          span.timestamp > withTimestampMicrosAfter &&
          span.duration > 0L

    def aChildSpan(withParentSpan: Span, withName: String, withTimestampMicrosAfter: Long): Span => Boolean =
      aChildSpan(
        withTraceIdHigh = withParentSpan.traceIdHigh,
        withTraceIdLow = withParentSpan.traceId,
        withParentSpanId = withParentSpan.id,
        withName,
        withTimestampMicrosAfter
      )

    def aChildSpan(withTraceIdHigh: Long, withTraceIdLow: Long, withParentSpanId: Long,
      withName: String, withTimestampMicrosAfter: Long): Span => Boolean =
      (span: Span) =>
        span.traceIdHigh == withTraceIdHigh &&
          span.traceId == withTraceIdLow &&
          span.parentId == withParentSpanId &&
          span.name == withName &&
          span.timestamp > withTimestampMicrosAfter &&
          span.duration > 0L &&
          span.id != span.parentId
  }

  /*
   * This is just here to facilitate seeing the span when working on this test.
   */
  private class DebugReporter(delegate: Reporter[Span]) extends Reporter[Span] with LazyLogging {
    override def report(span: Span): Unit = {
      logger.debug(s"Reported Span [$span]")
      delegate.report(span)
    }
  }
}