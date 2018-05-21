package support.matchers

import org.scalatest.matchers.{ BeMatcher, MatchResult }
import play.api.http.Status.{ INSUFFICIENT_STORAGE, INTERNAL_SERVER_ERROR }

/*
 * A matcher for HTTP status codes in the range: Server Error 5xx
 */
class HttpServerErrorStatusCodeMatcher extends BeMatcher[Int] {
  override def apply(left: Int): MatchResult =
    MatchResult(
      left >= INTERNAL_SERVER_ERROR && left <= INSUFFICIENT_STORAGE,
      s"$left was not a HTTP server error status code",
      s"$left was a HTTP server error status code"
    )
}

object HttpServerErrorStatusCodeMatcher {
  val aServerError = new HttpServerErrorStatusCodeMatcher()
}
