package utils

import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._

import uk.gov.ons.sbr.models._

/**
 * UriBuilder
 * ----------------
 * Author: haqa
 * Date: 16 August 2017 - 09:25
 * Copyright (c) 2017  Office for National Statistics
 */
// TODO - remove patch
object UriBuilder {

  private val periodPath = "periods"
  private val typePath = "types"
  private val unitPath = "units"

  /**
   *
   * @param baseUrl
   * @param units
   * @param periods
   * @param types
   * @param group - used to trigger Unit Type Search. If passed then assumed group is a string Unit Type to get vars.
   * @return
   */
  // @TODO - Remove group parameter
  def uriPathBuilder(baseUrl: String, units: String, periods: Option[String] = None, types: Option[DataSourceTypes] = None,
    group: String = ""): Uri = {
    val unitTypePath = DataSourceTypesUtil.fromString(group).getOrElse(None) match {
      case x: DataSourceTypes => x.path
      case _ => unitPath
    }
    (periods, types, units) match {
      case (Some(x), Some(y), z) => baseUrl / periodPath / x / typePath / y.toString / unitTypePath / z
      case (Some(x), None, z) =>
        if (List(VAT.toString, CRN.toString, PAYE.toString) contains group) {
          baseUrl / unitTypePath / z / periodPath / x
        } else {
          baseUrl / periodPath / x / unitTypePath / z
        }
      case (None, Some(y), z) =>
        baseUrl / typePath / y.toString / unitTypePath / z
      case _ => baseUrl / unitTypePath / units
    }
  }
}
