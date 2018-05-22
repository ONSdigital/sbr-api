package config

import com.typesafe.config.Config
import com.typesafe.sslconfig.util.ConfigLoader
import repository.sbrctrl.SbrCtrlUnitRepositoryConfig
import utils.url.BaseUrl

/*
 * We want a misconfigured server to "fail fast".
 * The Guice module should be configured to use this ConfigLoader during its configure method.
 * If any required key is missing / any value cannot be successfully parsed, an exception should be thrown
 * which will fail the startup of the service (at deployment time).
 */
object SbrCtrlUnitRepositoryConfigLoader extends ConfigLoader[SbrCtrlUnitRepositoryConfig] {
  def load(config: Config): SbrCtrlUnitRepositoryConfig =
    load(config, "api.sbr.control")

  override def load(rootConfig: Config, path: String): SbrCtrlUnitRepositoryConfig = {
    val config = rootConfig.getConfig(path)
    val baseUrl = BaseUrl(
      protocol = config.getString("protocol"),
      host = config.getString("host"),
      port = config.getInt("port")
    )
    SbrCtrlUnitRepositoryConfig(baseUrl)
  }
}
