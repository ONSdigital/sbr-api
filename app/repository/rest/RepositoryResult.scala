package repository.rest

import play.api.libs.json.{ JsValue, Reads }
import repository.ErrorMessage

object RepositoryResult {
  def as[T](reads: Reads[T])(errorOrOptJson: Either[ErrorMessage, Option[JsValue]]): Either[ErrorMessage, Option[T]] =
    errorOrOptJson.right.flatMap { optJson =>
      optJson.fold[Either[ErrorMessage, Option[T]]](Right(None)) { jsValue =>
        readJson(reads, jsValue).right.map(Some(_))
      }
    }

  private def readJson[T](reads: Reads[T], jsValue: JsValue): Either[ErrorMessage, T] = {
    val errorsOrT = jsValue.validate(reads).asEither
    errorsOrT.left.map(errors => s"Unable to parse json response [$errors]")
  }
}
