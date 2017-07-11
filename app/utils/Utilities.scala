package utils

import java.io.File

import org.slf4j.LoggerFactory
import play.api.libs.json.{ JsObject, Json }

import CsvProcessor._
import controllers.v1.{ MatchObj, Matches }

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
        logger.info(s"Found matching record with ${element} as data[${cols(cols.indexOf(element))}] identified as ${cols(cols.indexOf(element))} type")
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

  // unuseds
  def getStatUnit(id: String, source: String): JsObject = {
    Json.obj(
      "id" -> id,
      "source" -> source
    )
  }

  def formatterObj() = ???

}