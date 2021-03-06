package config

import com.typesafe.config.Config
import com.typesafe.sslconfig.util.ConfigLoader
import repository.rest.RestRepositoryConfig

/*
 * We want a misconfigured server to "fail fast".
 * The Guice module should be configured to use this ConfigLoader wrapper during its configure method.
 * If any required key is missing / any value cannot be successfully parsed, an exception should be thrown
 * which will fail the startup of the service (at deployment time).
 */
object SbrCtrlRestUnitRepositoryConfigLoader {
  def apply(restUnitRepositoryConfigLoader: ConfigLoader[RestRepositoryConfig], rootConfig: Config): RestRepositoryConfig =
    restUnitRepositoryConfigLoader.load(config = rootConfig, path = "api.sbr.control")
}
