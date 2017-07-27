package controllers.v1

import play.api.mvc.{ AnyContent, Controller, Request, Result }
import com.typesafe.scalalogging.StrictLogging
import models.units.{ Enterprise, EnterpriseObj }
import play.api.Play
import play.api.Play.getFile
import utils.CsvProcessor.readFile

import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }

/**
 * Created by haqa on 10/07/2017.
 */
trait ControllerUtils extends Controller with StrictLogging {

  protected val minLengthKey = 8
  //  protected def config: Config
  protected def getQueryString(request: Request[AnyContent]) = request.queryString.map { case v => v._2.mkString }

  protected[this] def errAsResponse(f: => Future[Result]): Future[Result] = Try(f) match {
    case Success(g) => g
    case Failure(err) =>
      logger.error("Unable to produce response.", err)
      Future.successful {
        InternalServerError(s"{err = '${err}'}")
      }
  }

  def findRecord(element: String, filename: String): List[Enterprise] = {
    val records = for {
      data <- readFile(filename)
      cols = data.split(",").map(_.trim)
      res: Option[Enterprise] = if (cols(1).matches(element)) {
        logger.info(s"Found matching record with ${element} " +
          s"as data[${cols(cols.indexOf(element))}] identified as ${cols(cols.indexOf(element))} type")
        Some(EnterpriseObj.fromMap(cols))
      } else {
        None
      }
    } yield (res)
    records.flatten.toList
  }

}
