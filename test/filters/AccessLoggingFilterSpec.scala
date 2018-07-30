package filters

import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FreeSpec, Matchers }
import play.api.mvc.{ Headers, RequestHeader, ResponseHeader }

class AccessLoggingFilterSpec extends FreeSpec with Matchers with MockFactory {

  private trait Fixture {
    val RequestMethod = "GET"
    val RequestUri = "/v1/periods/201806/ents/1234567890"
    val RequestRemoteAddress = "127.0.0.1"

    val requestHeader = stub[RequestHeader]
  }

  "An Access Log formatter" - {
    "builds an access log entry for an access event" - {
      "when Zipkin tracing headers are available" in new Fixture {
        val headers = new Headers(Seq(
          "X-B3-TraceId" -> "7ccace5dd505a64eaccb2f9d1a9a4739",
          "X-B3-SpanId" -> "6b2e08a9748184c9",
          "X-B3-ParentSpanId" -> "a58971547454d3dd"
        ))
        (requestHeader.headers _).when().returns(headers)
        (requestHeader.method _).when().returns(RequestMethod)
        (requestHeader.uri _).when().returns(RequestUri)
        (requestHeader.remoteAddress _).when().returns(RequestRemoteAddress)

        AccessLogFormatter(requestHeader, new ResponseHeader(status = 200)) shouldBe
          "traceId=[7ccace5dd505a64eaccb2f9d1a9a4739] parentSpanId=[a58971547454d3dd] spanId=[6b2e08a9748184c9] method=[GET] uri=[/v1/periods/201806/ents/1234567890] remote-address=[127.0.0.1] status=[200]"
      }

      "when Zipkin tracing headers are not available" in new Fixture {
        val headers = new Headers(Seq.empty)
        (requestHeader.headers _).when().returns(headers)
        (requestHeader.method _).when().returns(RequestMethod)
        (requestHeader.uri _).when().returns(RequestUri)
        (requestHeader.remoteAddress _).when().returns(RequestRemoteAddress)

        AccessLogFormatter(requestHeader, new ResponseHeader(status = 200)) shouldBe
          "traceId=[none] parentSpanId=[none] spanId=[none] method=[GET] uri=[/v1/periods/201806/ents/1234567890] remote-address=[127.0.0.1] status=[200]"
      }
    }
  }
}
