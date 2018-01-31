package utils

import java.io.File

import play.api.libs.json.{ Json, JsLookupResult, JsValue, JsObject, JsNull }

/**
 * UriBuilder
 * ----------------
 * Author: haqa & coolit
 * Date: 16 August 2017 - 09:25
 * Copyright (c) 2017  Office for National Statistics
 */
object Utilities {

  private def currentDirectory = new File(".").getCanonicalPath

  @deprecated("Migrated to errAsJson with 2 params", "feature/new-admin-routes - 31 January 2018")
  def errAsJson(status: Int, code: String, msg: String, cause: String): JsObject = {
    Json.obj(
      "status" -> status,
      "code" -> code,
      "route_with_cause" -> cause,
      "message_en" -> msg
    )
  }

  def errAsJson(msg: String, cause: String = "Not traced"): JsObject = {
    Json.obj(
      "route_with_cause" -> cause,
      "message_en" -> msg
    )
  }

  def getElement(value: AnyRef) = {
    val res = value match {
      case None => ""
      case Some(i: Int) => i
      case Some(l: Long) => l
      case Some(z) => s"""${z.toString}"""
    }
    res
  }

  def unquote(s: String) = s.replace("\"", "")

  implicit class orElseNull(val j: JsLookupResult) {
    def getOrNull: JsValue = j.getOrElse(JsNull)
  }

}