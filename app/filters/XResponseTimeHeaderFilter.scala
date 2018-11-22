package filters

import akka.stream.Materializer
import controllers.BuildInfo
import javax.inject.Inject
import play.Configuration
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

class XResponseTimeHeaderFilter @Inject() (implicit val mat: Materializer, ec: ExecutionContext, config: Configuration) extends Filter {
  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    val startTime = System.currentTimeMillis

    nextFilter(requestHeader).map { result =>
      val endTime = System.currentTimeMillis
      val responseTime = endTime - startTime

      if (config.getBoolean("play.filters.cors.allowAll")) {
        result.withHeaders(
          "X-Response-Time" -> responseTime.toString,
          "Server" -> (BuildInfo.name + "/" + BuildInfo.version),
          "Access-Control-Allow-Origin" -> "*",
          "Access-Control-Allow-Methods" -> "OPTIONS, GET, POST, PUT, DELETE, HEAD",
          "Access-Control-Allow-Headers" -> "Accept, Content-Type, Origin, X-Json, X-Prototype-Version, X-Requested-With",
          "Access-Control-Allow-Credentials" -> "true"
        )
      } else {
        result.withHeaders(
          "X-Response-Time" -> responseTime.toString,
          "Server" -> (BuildInfo.name + "/" + BuildInfo.version)
        )
      }
    }
  }
}
