package utils

import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import play.api.libs.json.JsValue
import uk.gov.ons.sbr.models._

/**
 * UriBuilder
 * ----------------
 * Author: haqa
 * Date: 16 August 2017 - 09:25
 * Copyright (c) 2017  Office for National Statistics
 */
object UriBuilder {

  private val PERIOD_PATH = "periods"
  private val TYPE_PATH = "types"
  private val UNIT_PATH = "units"
  private val HISTORY_PATH = "history"
  private val HISTORY_MAX_ARG = "max"

  /**
   *
   * @param baseUrl - url of the api.
   * @param units - id
   * @param periods - Optional
   * @param types - Optional
   * @param group - used to trigger Unit Type Search. If passed then assumed group is a string Unit Type to get vars.
   * @param history - Optional, limits the result size when period isn't given.
   * @return Uri
   */
  // @TODO - Remove group parameter
  def createUri(baseUrl: String, units: String, periods: Option[String] = None, types: Option[DataSourceTypes] = None,
    group: String = "", history: Option[Int] = None): Uri = {
    val unitTypePath = DataSourceTypesUtil.fromString(group).getOrElse(None) match {
      case x: DataSourceTypes => x.path
      case _ => UNIT_PATH
    }
    (periods, types, units, history) match {
      case (Some(p), Some(t), u, None) => baseUrl / PERIOD_PATH / p / TYPE_PATH / t.toString / unitTypePath / u
      case (Some(p), None, u, None) => if (List(VAT.toString, CRN.toString, PAYE.toString, LEU.toString) contains group) {
        baseUrl / unitTypePath / u / PERIOD_PATH / p
      } else {
        baseUrl / PERIOD_PATH / p / unitTypePath / u
      }
      // TODO **WARN** ~ this will break for ENTERPRISE until history param arg route is add to sbr-control ~ **WARN**
      case (None, None, u, Some(h)) => baseUrl / unitTypePath / u / HISTORY_PATH ? (HISTORY_MAX_ARG -> h)
      case (None, Some(t), u, None) => baseUrl / TYPE_PATH / t.toString / unitTypePath / u
      case _ => baseUrl / unitTypePath / units
    }
  }

  def createLouPeriodUri(baseUrl: String, id: String, period: String): Uri =
    baseUrl / PERIOD_PATH / period / TYPE_PATH / LOU.toString / UNIT_PATH / id

  def createLouUri(baseUrl: String, id: String, unitLinks: JsValue): Uri = {
    val entId = (unitLinks \ "parents" \ "ENT").as[String]
    val period = (unitLinks \ "period").as[String]
    baseUrl / "enterprises" / entId / PERIOD_PATH / period / "localunits" / id
  }
}
