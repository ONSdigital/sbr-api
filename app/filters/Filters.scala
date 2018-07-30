package filters

import javax.inject.{ Inject, Named }
import play.api.http.DefaultHttpFilters
import play.api.mvc.Filter
import play.filters.gzip.GzipFilter
import tracing.TracingFilterName

class Filters @Inject() (
  responseTimeHeader: XResponseTimeHeaderFilter,
  accessLoggingFilter: AccessLoggingFilter,
  @Named(TracingFilterName) traceFilter: Filter,
  gzipFilter: GzipFilter
) extends DefaultHttpFilters(responseTimeHeader, traceFilter, accessLoggingFilter, gzipFilter)