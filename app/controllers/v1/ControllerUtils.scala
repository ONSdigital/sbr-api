package controllers.v1

import com.typesafe.config.Config
import play.api.mvc.{ Controller, Result }
import com.typesafe.scalalogging.StrictLogging

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }

/**
 * Created by haqa on 10/07/2017.
 */
trait ControllerUtils extends Controller with StrictLogging {

  protected def config: Config

  @tailrec
  final protected def buildErrMsg(x: Throwable, msgs: List[String] = Nil): String = {
    Option(x.getCause) match {
      case None => (x.getMessage :: msgs).reverse.mkString(" ")
      case Some(ex) => buildErrMsg(ex, x.getMessage :: msgs)
    }
  }

  protected[this] def errAsResponse(f: => Future[Result]): Future[Result] = Try(f) match {
    case Success(g) => g
    case Failure(err) =>
      logger.error("Unable to produce response.", err)
      Future.successful {
        InternalServerError(s"{err = '${buildErrMsg(err)}'}")
      }
  }

}
