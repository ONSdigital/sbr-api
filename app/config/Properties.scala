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
  lazy val host: String = config.getString("leuSourceHost")
  lazy val requestTimeout: Int = config.getInt("requestTimeout")
  lazy val minKeyLength: Int = config.getInt("minLengthKey")
  lazy val controlEndpoint: String = config.getString("sbrControlApi")
  lazy val baseSearchRoute: String = config.getString("sbrApiBaseSearch")
  lazy val businessIndexRoute: String = config.getString("biApi")
  lazy val controlEnterpriseSearch: String = config.getString("sbrControlEnt")
  lazy val adminCompaniesSearch: String = config.getString("sbrAdminCrn")
  lazy val adminVATsSearch: String = config.getString("sbrAdminVat")
  lazy val adminPAYEsSearch: String = config.getString("sbrAdminPaye")
  lazy val enterpriseSearchWithPeriod: String = config.getString("sbrControlEntPeriod")
  lazy val adminVATsSearchWithPeriod: String = config.getString("sbrAdminVatPeriod")
  lazy val adminPAYEsSearchWithPeriod: String = config.getString("sbrAdminPayePeriod")
  lazy val adminCompaniesSearchWithPeriod: String = config.getString("sbrAdminCrnPeriod")

}
