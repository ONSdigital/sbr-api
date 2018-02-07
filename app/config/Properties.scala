package config

import play.api.Configuration
import com.typesafe.config.Config

/**
 * Created by haqa on 28/07/2017.
 */

/**
 * @todo - fix config vals based on environment unforced default issue
 */

trait Properties {
  implicit val configuration: Configuration
  lazy private val config: Config = configuration.underlying

  lazy val requestTimeout: Long = config.getInt("request.timeout")
  lazy val minKeyLength: Int = config.getInt("minimum.key.length")

  // SBR CONTROL API
  lazy val controlEditEntURL: String = config.getString("api.sbr.control.edit.enterprise")
  lazy val controlEditEntWithPeriodURL: String = config.getString("api.sbr.control.edit.enterprise.with.period")
  lazy val sbrControlApiURL: String = config.getString("api.sbr.control.url")
  // ADMIN DATA APIs
  lazy val chAdminDataApiURL: String = config.getString("api.sbr.admin.data.ch.url")
  lazy val vatAdminDataApiURL: String = config.getString("api.sbr.admin.data.vat.url")
  lazy val payeAdminDataApiURL: String = config.getString("api.sbr.admin.data.paye.url")
  // BUSINESS INDEX API
  lazy val businessIndexDataApiURL: String = config.getString("api.business.index.data.url")
}
