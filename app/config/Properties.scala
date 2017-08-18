package config

import com.typesafe.config.{ Config, ConfigFactory }

/**
 * Created by haqa on 28/07/2017.
 */

/**
 * @todo - fix config vals based on environment
 *       unforced default issue
 */

object Properties {

  private val config: Config = SBRPropertiesConfiguration.envConfig(ConfigFactory.load())

  lazy val host: String = config.getString("legal.units.source.host")
  lazy val requestTimeout: Int = config.getInt("request.timeout")
  lazy val minKeyLength: Int = config.getInt("minLengthKey")
  lazy val controlEndpoint: String = config.getString("sbr.control.api")
  lazy val baseSearchRoute: String = config.getString("sbr.api.base.search")
  lazy val businessIndexRoute: String = config.getString("bi.api")
  lazy val controlEnterpriseSearch: String = config.getString("sbr.api.control.enterprise.search")
  lazy val adminCompaniesSearch: String = config.getString("sbr.admin.data.api.crn")
  lazy val adminVATsSearch: String = config.getString("sbr.admin.data.api.vat")
  lazy val adminPAYEsSearch: String = config.getString("sbr.admin.data.api.paye")
  lazy val controlEnterpriseSearchWithPeriod: String = config.getString("sbr.api.control.enterprise.search.with.period")
  lazy val adminVATsSearchWithPeriod: String = config.getString("sbr.admin.data.api.vat.with.period")
  lazy val adminPAYEsSearchWithPeriod: String =config.getString("sbr.admin.data.api.paye.with.period")
  lazy val adminCompaniesSearchWithPeriod: String =config.getString("sbr.admin.data.api.crn.with.period")




}
