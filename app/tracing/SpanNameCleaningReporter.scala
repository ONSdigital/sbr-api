package tracing

import tracing.SpanNameCleaningReporter.regexPathParam
import zipkin.Span
import zipkin.reporter.Reporter

/*
 * A Zipkin Reporter that "cleans up" spanNames by removing regex definitions and then forwards the modified span
 * to a delegate reporter.
 */
class SpanNameCleaningReporter(delegate: Reporter[Span]) extends Reporter[Span] {
  override def report(span: Span): Unit =
    delegate.report(modifySpan(span))

  private def modifySpan(span: Span): Span = {
    val builder = span.toBuilder
    builder.name(withoutRegex(span.name)).build()
  }

  /*
   * Strip any regular expressions from dynamic routes, converting $id<regex> to $id.
   */
  private def withoutRegex(name: String): String = {
    val params = regexPathParam.findAllIn(name)
    params.matchData.foldLeft(name) { (acc, param) =>
      acc.replace(param.matched, param.group(1))
    }
  }
}

object SpanNameCleaningReporter {
  /*
   * We want to convert $id<regex> to $id.
   * \$[^<]+ is the $id component.  We make a capturing group for this by wrapping in parentheses (\$[^<]+)
   * <[^>]+> is the <regex> component.
   */
  private val regexPathParam = """(\$[^<]+)<[^>]+>""".r
}