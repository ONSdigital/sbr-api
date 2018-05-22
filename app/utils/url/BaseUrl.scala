package utils.url

case class BaseUrl(protocol: String, host: String, port: Int)

object BaseUrl {
  object Protocol {
    val Http = "http"
  }

  def asUrlString(baseUrl: BaseUrl): String =
    s"${baseUrl.protocol}://${baseUrl.host}:${baseUrl.port}"
}