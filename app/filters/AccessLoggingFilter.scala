package filters

import akka.stream.Materializer
import com.typesafe.scalalogging.LazyLogging
import javax.inject.Inject
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/*
 * See https://www.playframework.com/documentation/2.5.x/ScalaLogging
 */
class AccessLoggingFilter @Inject() (implicit val mat: Materializer, ec: ExecutionContext) extends Filter with LazyLogging {
  def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {
    val resultFuture = next(request)
    resultFuture.foreach { result =>
      logger.info(AccessLogFormatter(request, result.header))
    }

    resultFuture
  }
}

object AccessLogFormatter {
  private val TraceId = "X-B3-TraceId"
  private val SpanId = "X-B3-SpanId"
  private val ParentSpanId = "X-B3-ParentSpanId"

  def apply(request: RequestHeader, response: ResponseHeader): String = {
    val valueOfHeader = headerValueOrElseNone(request.headers) _
    Seq(
      keyValueDescription("traceId", valueOfHeader(TraceId)),
      keyValueDescription("parentSpanId", valueOfHeader(ParentSpanId)),
      keyValueDescription("spanId", valueOfHeader(SpanId)),
      keyValueDescription("method", request.method),
      keyValueDescription("uri", request.uri),
      keyValueDescription("remote-address", request.remoteAddress),
      keyValueDescription("status", response.status.toString)
    ).mkString(start = "", sep = " ", end = "")
  }

  private def headerValueOrElseNone(headers: Headers)(headerName: String): String =
    headers.get(headerName).getOrElse("none")

  private def keyValueDescription(key: String, value: String): String =
    s"$key=[$value]"
}