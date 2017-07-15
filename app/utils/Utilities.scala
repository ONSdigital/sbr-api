package utils

import java.io.File

import org.slf4j.LoggerFactory
import play.api.libs.json._
import CsvProcessor._
import models.records.attributes.{ Address, Matches }

/**
 * Created by haqa on 05/07/2017.
 */
object Utilities {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  def findRecord(element: String, filename: String): List[Matches] = {
    val records = for {
      data <- readFile(filename)
      cols = data.split(",").map(_.trim)
      res: Option[Matches] = if (cols.contains(element)) {
        logger.info(s"Found matching record with ${element} " +
          s"as data[${cols(cols.indexOf(element))}] identified as ${cols(cols.indexOf(element))} type")
        Some(MatchObj.fromMap(cols))
      } else {
        None
      }
    } yield (res)
    records.flatten.toList
  }

  def currentDirectory = new File(".").getCanonicalPath

  def errAsJson(status: Int, code: String, msg: String): JsObject = {
    Json.obj(
      "status" -> status,
      "code" -> code,
      "message_en" -> msg
    )
  }

  def getElement(value: Any) = {
    val res = value match {
      case Some(i: Int) => i
      case Some(l: Long) => l
      case Some(z) => s""""${z}""""
      case x => s""""${x.toString}""""
      case None => ""
    }
    res
  }

  /**
   * UNUSED - For future use.
   */
  def getStatUnit(id: String, source: String): JsObject = {
    Json.obj(
      "id" -> id,
      "source" -> source
    )
  }

  //  def formatterObj(json : JsValue) : Future[Result] = {
  //    Json.fromJson[Matches](json) match {
  //      case JsSuccess(matchObj, _) => {
  //        logger.debug(s"Feedback Received: $matchObj")
  //        Ok("Success").future
  //      }
  //      case JsError(err) => {
  //        logger.error(s"Invalid Feedback! Please give properly parsable feedback $json -> $err")
  //        BadRequest(errAsJson(400, "invalid_input", s"Invalid Feedback! Please give properly parsable feedback $json -> $err")).future
  //      }
  //    }
  //  }

}