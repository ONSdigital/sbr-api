package config

import play.api.Configuration
import com.typesafe.config.Config

trait Properties {
  implicit val configuration: Configuration
  lazy private val CONFIG: Config = configuration.underlying

  // Utils
  lazy val MINIMUM_KEY_LENGTH: Int = CONFIG.getInt("minimum.key.length")
  // SBR CONTROL API
  lazy val CONTROL_EDIT_ENTERPRISE_URL: String = CONFIG.getString("api.sbr.control.edit.enterprise")
  lazy val SBR_CONTROL_API_URL: String = CONFIG.getString("api.sbr.control.url")
  // ADMIN DATA APIs
  lazy val CH_ADMIN_DATA_API_URL: String = CONFIG.getString("api.sbr.admin.data.ch.url")
  lazy val VAT_ADMIN_DATA_API_URL: String = CONFIG.getString("api.sbr.admin.data.vat.url")
  lazy val PAYE_ADMIN_DATA_API_URL: String = CONFIG.getString("api.sbr.admin.data.paye.url")
  // BUSINESS INDEX API
  lazy val LEGAL_UNIT_DATA_API_URL: String = CONFIG.getString("api.business.index.data.url")
}
