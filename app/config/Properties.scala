package config

import play.api.Configuration
import com.typesafe.config.Config

/**
 * Properties
 * ----------------
 * Author: haqa
 * Date: 10 July 2017 - 09:25
 * Copyright (c) 2017  Office for National Statistics
 */
trait Properties {
  implicit val configuration: Configuration
  lazy private val CONFIG: Config = configuration.underlying

  // Utils
  lazy val API_REQUEST_TIMEOUT: Long = CONFIG.getInt("request.timeout")
  lazy val MINIMUM_KEY_LENGTH: Int = CONFIG.getInt("minimum.key.length")
  // SBR CONTROL API
  lazy val CONTROL_EDIT_ENTERPRISE_URL: String = CONFIG.getString("api.sbr.control.edit.enterprise")
  // TODO - REMOVE if uneeded controlEditEntWithPeriodURL
  lazy val controlEditEntWithPeriodURL: String = CONFIG.getString("api.sbr.control.edit.enterprise.with.period")
  lazy val SBR_CONTROL_API_URL: String = CONFIG.getString("api.sbr.control.url")
  // ADMIN DATA APIs
  lazy val CH_ADMIN_DATA_API_URL: String = CONFIG.getString("api.sbr.admin.data.ch.url")
  lazy val VAT_ADMIN_DATA_API_URL: String = CONFIG.getString("api.sbr.admin.data.vat.url")
  lazy val PAYE_ADMIN_DATA_API_URL: String = CONFIG.getString("api.sbr.admin.data.paye.url")
  // BUSINESS INDEX API
  lazy val LEGAL_UNIT_DATA_API_URL: String = CONFIG.getString("api.business.index.data.url")
}
