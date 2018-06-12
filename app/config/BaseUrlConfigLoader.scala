package config

import com.typesafe.config.Config
import com.typesafe.sslconfig.util.ConfigLoader
import utils.url.BaseUrl

object BaseUrlConfigLoader extends ConfigLoader[BaseUrl] {
  override def load(rootConfig: Config, path: String): BaseUrl = {
    val config = rootConfig.getConfig(path)
    BaseUrl(
      protocol = config.getString("protocol"),
      host = config.getString("host"),
      port = config.getInt("port")
    )
  }
}
