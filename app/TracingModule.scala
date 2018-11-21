import actions.{TracedRequest, WithTracingAction}
import com.google.inject.name.Names.named
import com.google.inject.{AbstractModule, Provides, TypeLiteral}
import config.BaseUrlConfigLoader
import jp.co.bizreach.trace.ZipkinTraceServiceLike
import jp.co.bizreach.trace.play.filter.ZipkinTraceFilter
import play.api.Mode.{Dev, Prod, Test}
import play.api.mvc.{ActionBuilder, AnyContent, Filter}
import play.api.{Configuration, Environment}
import tracing._
import utils.url.Url
import zipkin2.Span
import zipkin2.reporter.okhttp3.OkHttpSender
import zipkin2.reporter.{AsyncReporter, Reporter}

/*
 * An attempt to keep all configuration relating to tracing in an isolated module, as this is not a core
 * feature of the application.
 */
class TracingModule(environment: Environment, configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[ZipkinTraceServiceLike]).to(classOf[ZipkinTraceService])
    bind(classOf[Filter]).annotatedWith(named(TracingFilterName)).to(classOf[ZipkinTraceFilter])
    bind(classOf[TraceWSClient]).to(classOf[ZipkinTraceWSClient])
    bind(new TypeLiteral[ActionBuilder[TracedRequest, AnyContent]]() {}).to(classOf[WithTracingAction])
    () // Explicitly return unit to avoid warning about discarded non-Unit value.
  }

  /*
   * Note that we only try to send the trace to a Zipkin server if the application is running in "production" mode.
   * This allows developers to easily run the application without a Zipkin server (traces will be printed to the console).
   * We wrap the "real reporter" with one that cleans-up span names by removing regex definitions.
   */
  @Provides
  def providesZipkinReporter: Reporter[Span] =
    new SpanNameCleaningReporter(delegateZipkinReporter)

  private def delegateZipkinReporter: Reporter[Span] =
    environment.mode match {
      case Dev => Reporter.CONSOLE
      case Test => Reporter.CONSOLE
      case Prod => zipkinHttpReporter
    }

  private def zipkinHttpReporter: Reporter[Span] = {
    val baseUrl = BaseUrlConfigLoader.load(configuration.underlying, "trace.zipkin.reporter")
    val reporterUrl = Url(withBase = baseUrl, withPath = "api/v1/spans")
    AsyncReporter.builder(OkHttpSender.create(reporterUrl)).build()
  }
}
