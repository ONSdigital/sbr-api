package tracing
import java.io.IOException

import javax.inject.Inject
import play.api.libs.ws.WSRequest

/*
 * This class simply wraps the play-zipkin-tracing implementation with our own trait.
 */
class ZipkinTraceWSClient @Inject() (wsClient: jp.co.bizreach.trace.play25.TraceWSClient) extends TraceWSClient {
  override def url(url: String, spanName: String, traceData: TraceData): WSRequest =
    wsClient.url(spanName, url)(jp.co.bizreach.trace.TraceData(traceData.asSpan))

  /** Closes this client, and releases underlying resources. */
  @throws[IOException] override def close(): Unit =
    wsClient.close()
}
