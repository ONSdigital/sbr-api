package utils

import play.api.libs.json._

/**
 * UriBuilder
 * ----------------
 * Author: haqa & coolit
 * Date: 16 August 2017 - 09:25
 * Copyright (c) 2017  Office for National Statistics
 */
object Utilities {

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

  def unquote(s: String): String = s.replace("\"", "")

  implicit class orElseNull(val j: JsLookupResult) {
    def getOrNull: JsValue = j.getOrElse(JsNull)
  }

}