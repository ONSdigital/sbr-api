package config

import com.typesafe.config.Config
import com.typesafe.sslconfig.util.ConfigLoader
import repository.rest.RestUnitRepositoryConfig

/*
 * We want a misconfigured server to "fail fast".
 * The Guice module should be configured to use this ConfigLoader wrapper during its configure method.
 * If any required key is missing / any value cannot be successfully parsed, an exception should be thrown
 * which will fail the startup of the service (at deployment time).
 */
object RestAdminDataRepositoryConfigLoader {
  private val adminDataPath = "api.admin.data"

  def vat(restUnitRepositoryConfigLoader: ConfigLoader[RestUnitRepositoryConfig], rootConfig: Config): RestUnitRepositoryConfig =
    restUnitRepositoryConfigLoader.load(config = rootConfig, path = s"$adminDataPath.vat")

  def paye(restUnitRepositoryConfigLoader: ConfigLoader[RestUnitRepositoryConfig], rootConfig: Config): RestUnitRepositoryConfig =
    restUnitRepositoryConfigLoader.load(config = rootConfig, path = s"$adminDataPath.paye")

  def companiesHouse(restUnitRepositoryConfigLoader: ConfigLoader[RestUnitRepositoryConfig], rootConfig: Config): RestUnitRepositoryConfig =
    restUnitRepositoryConfigLoader.load(config = rootConfig, path = s"$adminDataPath.ch")
}
