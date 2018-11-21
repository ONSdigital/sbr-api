package tracing

import java.lang.System.currentTimeMillis

import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FreeSpec, Matchers }
import zipkin2.Span
import zipkin2.reporter.Reporter

class SpanNameCleaningReporterSpec extends FreeSpec with Matchers with MockFactory {

  private trait Fixture {
    private val spanBuilder = Span.builder().
      traceId(1122334455L).
      id(98765L).
      parentId(123456L).
      timestamp(currentTimeMillis()).
      duration(500L).
      debug(false)

    private val delegateReporter = mock[Reporter[Span]]
    private val reporter = new SpanNameCleaningReporter(delegateReporter)

    def expectRouteGeneratesSpanWithName(route: String, spanName: String): Unit = {
      val span = spanBuilder.name(route).build()

      // expectation is only met if the spanName has been correctly modified and the spanName is the only modification
      (delegateReporter.report _).expects(where {
        (reportedSpan: Span) =>
          reportedSpan.name == spanName &&
            reportedSpan.toBuilder.name(route).build() == span
      })

      reporter.report(span)
    }
  }

  "A SpanNameCleaningReporter" - {
    "reports the span with an unmodified name" - {
      "when the route definition is a static path" in new Fixture {
        val route = "/health"
        expectRouteGeneratesSpanWithName(route = route, spanName = route)
      }

      "when the route definition contains simple dynamic path parameters" in new Fixture {
        val route = "/v1/periods/:period/ents/:ern"
        expectRouteGeneratesSpanWithName(route = route, spanName = route)
      }
    }

    "reports the span with a modified name" - {
      "when the route definition contains dynamic path parameters with custom regular expressions" in new Fixture {
        expectRouteGeneratesSpanWithName(
          route = """/v1/periods/$period<\d{4}(0[1-9]|1[0-2])>/ents/$ern<\d{10}>""",
          spanName = "/v1/periods/$period/ents/$ern"
        )
      }
    }
  }
}
