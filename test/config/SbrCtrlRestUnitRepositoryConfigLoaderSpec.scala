package config

import com.typesafe.config.ConfigException.Missing
import com.typesafe.config.{ ConfigException, ConfigFactory }
import com.typesafe.sslconfig.util.ConfigLoader
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FreeSpec, Matchers }
import repository.rest.RestUnitRepositoryConfig
import utils.url.BaseUrl
import utils.url.BaseUrl.Protocol.Http

class SbrCtrlRestUnitRepositoryConfigLoaderSpec extends FreeSpec with Matchers with MockFactory {

  private trait Fixture {
    val ExpectedConfigPath = "api.sbr.control"
    val SomeConfig = ConfigFactory.parseString("")
    val SomeRestUnitRepositoryConfig = RestUnitRepositoryConfig(BaseUrl(protocol = Http, host = "localhost", port = 8080))

    val restUnitRepositoryConfigLoader = mock[ConfigLoader[RestUnitRepositoryConfig]]
  }

  "The config for an SBR Control unit repository" - {
    "can be successfully loaded when it contains a valid configuration" in new Fixture {
      (restUnitRepositoryConfigLoader.load _).expects(SomeConfig, ExpectedConfigPath).returning(SomeRestUnitRepositoryConfig)

      SbrCtrlRestUnitRepositoryConfigLoader(restUnitRepositoryConfigLoader)(SomeConfig) shouldBe SomeRestUnitRepositoryConfig
    }

    "cannot be loaded when it contains an invalid configuration" in new Fixture {
      (restUnitRepositoryConfigLoader.load _).expects(SomeConfig, ExpectedConfigPath).throwing(new Missing(ExpectedConfigPath))

      a[ConfigException] shouldBe thrownBy {
        SbrCtrlRestUnitRepositoryConfigLoader(restUnitRepositoryConfigLoader)(SomeConfig)
      }
    }
  }
}
