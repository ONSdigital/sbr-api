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

  // @TODO - Remove group parameter
  def uriPathBuilder(baseUrl: String, units: String, periods: Option[String] = None, types: Option[DataSourceTypes] = None,
    group: String = ""): Uri = {
    val unitTypePath = DataSourceTypesUtil.fromString(group.toUpperCase).getOrElse(None) match {
      case x: DataSourceTypes => x.path
      case _ => "units"
    }
    (periods, types, units) match {
      case (Some(x), Some(y), z) => baseUrl / periodPath / x / typePath / crnToCHPatch(y) / unitTypePath / z
      case (Some(x), None, z) =>
        if (List(VAT.toString, CRN.toString, PAYE.toString) contains group) {
          baseUrl / unitTypePath / z / periodPath / x
        } else {
          baseUrl / periodPath / x / unitTypePath / z
        }
      case (None, Some(y), z) =>
        baseUrl / typePath / crnToCHPatch(y) / unitTypePath / z
      case _ => baseUrl / unitTypePath / units
    }
  }

  // @NOTEu - crn patch
  def crnToCHPatch(`type`: DataSourceTypes): String = `type` match { case CRN => CRN.shortName case _ => `type`.toString }

}
