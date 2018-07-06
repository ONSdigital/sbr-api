
package object tracing {
  // must be final to generate byte-code that will be considered "constant" so that we can use this with @Named annotation
  final val TracingFilterName = "trace-filter"
}
