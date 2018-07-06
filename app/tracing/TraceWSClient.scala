package tracing

import java.io.{ Closeable, IOException }

import play.api.libs.ws.WSRequest

/*
 * While play-zipkin-tracing provides a tracing wsClient, it provides only a concrete class and no associated trait.
 * Using the play-zipkin-tracing component directly would:
 * - limit our implementation options going forwards
 * - prevent us from being able to use mocks in tests (we do not mock concrete classes with scalamock).
 *   (note that we already have tests that are written against a mock Play wsClient - as the Play WSClient separates
 *    the interface from the implementation)
 *
 * We therefore provide our own trait for this behaviour, and provide an implementation that simply wraps the
 * play-zipkin-tracing implementation.
 *
 * @see tracing.ZipkinTraceWSClient
 * @see play.api.libs.ws.WSClient
  */
trait TraceWSClient extends Closeable {
  def url(url: String, spanName: String, traceData: TraceData): WSRequest

  /** Closes this client, and releases underlying resources. */
  @throws[IOException] def close(): Unit
}
