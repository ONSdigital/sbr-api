package utils

import org.slf4j.LoggerFactory
import utils.Utilities.currentDirectory

import scala.io.Source
import scala.util.{ Failure, Success, Try }

/**
 * Created by haqa on 05/07/2017.
 */
object CsvProcessor {

  private[this] val logger = LoggerFactory.getLogger(getClass)

  /**
   * @todo replace relative file finder
   */

  def readFile(filename: String): Iterator[String] = {
    logger.info(s"Reading csv with filename: ${filename.substring(filename.lastIndexOf("/") + 1)} -> at path ${currentDirectory}${filename}")
    val lines = Try(Source.fromInputStream(getClass.getResourceAsStream(filename)).getLines()) match {
      case Success(s) => s
      case Failure(ex) => throw new RuntimeException(s"Can't read file $filename", ex)
    }
    lines
  }

  def printAll(data: Iterator[String]) = {
    while (data.hasNext) {
      println(data.next())
    }
  }

}
