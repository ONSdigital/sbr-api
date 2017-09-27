package config

import com.typesafe.config.{ Config, ConfigFactory }

/**
 * Created by haqa on 28/07/2017.
 */

/**
 * @todo - fix config vals based on environment unforced default issue
 */

object Properties {

  private val config: Config = SBRPropertiesConfiguration.envConfig(ConfigFactory.load())

  lazy val leuSourceHost: String = config.getString("leuSourceHost")
  lazy val requestTimeout: Long = config.getInt("requestTimeout")
  lazy val minKeyLength: Int = config.getInt("minLengthKey")

  lazy val sbrControlApiBase: String = config.getString("sbrControlApiBase")
  lazy val sbrAdminBase: String = config.getString("sbrAdminBase")
  lazy val biBase: String = config.getString("biBase")

}
