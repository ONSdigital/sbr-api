package utils

import play.api.libs.json.{ JsError, JsResult, JsSuccess }

import scala.util.Try

object JsResultSupport {
  def fromTry[A](tryA: Try[A]): JsResult[A] =
    TrySupport.fold(tryA)(
      err => JsError(err.getMessage),
      a => JsSuccess(a)
    )

  def fromOption[A](optA: Option[A]): JsResult[A] =
    optA.fold[JsResult[A]](JsError()) { a =>
      JsSuccess(a)
    }
}
