package config

import com.typesafe.config.ConfigException.Missing
import com.typesafe.config.{ ConfigException, ConfigFactory }
import com.typesafe.sslconfig.util.ConfigLoader
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FreeSpec, Matchers }
import repository.rest.RestUnitRepositoryConfig
import utils.url.BaseUrl
import utils.url.BaseUrl.Protocol.Http

class VatRestAdminDataRepositoryConfigLoaderSpec extends FreeSpec with Matchers with MockFactory {

  private trait Fixture {
    val ExpectedConfigPath = "api.admin.data.vat"
    val SomeConfig = ConfigFactory.parseString("")
    val SomeRestUnitRepositoryConfig = RestUnitRepositoryConfig(BaseUrl(protocol = Http, host = "localhost", port = 8080))

    val restUnitRepositoryConfigLoader = mock[ConfigLoader[RestUnitRepositoryConfig]]
  }

  "The config for a VAT admin data repository" - {
    "can be successfully loaded when it contains a valid configuration" in new Fixture {
      (restUnitRepositoryConfigLoader.load _).expects(SomeConfig, ExpectedConfigPath).returning(SomeRestUnitRepositoryConfig)

      VatRestAdminDataRepositoryConfigLoader(restUnitRepositoryConfigLoader)(SomeConfig) shouldBe SomeRestUnitRepositoryConfig
    }

    "cannot be loaded when it contains an invalid configuration" in new Fixture {
      (restUnitRepositoryConfigLoader.load _).expects(SomeConfig, ExpectedConfigPath).throwing(new Missing(ExpectedConfigPath))

      a[ConfigException] shouldBe thrownBy {
        VatRestAdminDataRepositoryConfigLoader(restUnitRepositoryConfigLoader)(SomeConfig)
      }
    }
  }
}
