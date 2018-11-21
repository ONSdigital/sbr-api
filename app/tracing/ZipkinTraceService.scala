package tracing

import akka.actor.ActorSystem
import brave.Tracing
import brave.sampler.Sampler
import brave.sampler.Sampler.ALWAYS_SAMPLE
import javax.inject.{ Inject, Singleton }
import jp.co.bizreach.trace.ZipkinTraceConfig.{ AkkaName, ZipkinSampleRate }
import jp.co.bizreach.trace.ZipkinTraceServiceLike
import play.api.Configuration
import zipkin2.Span
import zipkin2.reporter.Reporter

import scala.concurrent.ExecutionContext

/*
 * This is based on jp.co.bizreach.trace.play25.ZipkinTraceService from play-zipkin-tracing.
 * The key difference is that we inject a reporter for flexibility, rather than hard-coding an asynchronous HTTP reporter.
 */
@Singleton
class ZipkinTraceService @Inject() (conf: Configuration, actorSystem: ActorSystem, reporter: Reporter[Span]) extends ZipkinTraceServiceLike {
  override implicit val executionContext: ExecutionContext = actorSystem.dispatchers.lookup(AkkaName)

  override val tracing: Tracing = Tracing.newBuilder()
    .localServiceName("sbr-api")
    .spanReporter(reporter)
    .sampler(samplerFrom(conf))
    .build()

  private def samplerFrom(conf: Configuration): Sampler = {
    val sampleRateOpt = conf.getString(ZipkinSampleRate).map(_.toFloat)
    sampleRateOpt.fold(ALWAYS_SAMPLE)(Sampler.create)
  }
}
