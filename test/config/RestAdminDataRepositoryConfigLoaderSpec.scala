package config

import com.typesafe.config.ConfigException.Missing
import com.typesafe.config.{ ConfigException, ConfigFactory }
import com.typesafe.sslconfig.util.ConfigLoader
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FreeSpec, Matchers }
import repository.rest.RestUnitRepositoryConfig
import utils.url.BaseUrl
import utils.url.BaseUrl.Protocol.Http

class RestAdminDataRepositoryConfigLoaderSpec extends FreeSpec with Matchers with MockFactory {

  private trait Fixture {
    val TheConfig = ConfigFactory.parseString("")
    val SomeRestUnitRepositoryConfig = RestUnitRepositoryConfig(BaseUrl(protocol = Http, host = "localhost", port = 8080))

    val restUnitRepositoryConfigLoader = mock[ConfigLoader[RestUnitRepositoryConfig]]
  }

  "The config for an admin data repository" - {
    "when valid" - {
      "can be loaded for VAT" in new Fixture {
        (restUnitRepositoryConfigLoader.load _).expects(TheConfig, "api.admin.data.vat").returning(SomeRestUnitRepositoryConfig)

        RestAdminDataRepositoryConfigLoader.vat(restUnitRepositoryConfigLoader, TheConfig) shouldBe SomeRestUnitRepositoryConfig
      }

      "can be loaded for PAYE" in new Fixture {
        (restUnitRepositoryConfigLoader.load _).expects(TheConfig, "api.admin.data.paye").returning(SomeRestUnitRepositoryConfig)

        RestAdminDataRepositoryConfigLoader.paye(restUnitRepositoryConfigLoader, TheConfig) shouldBe SomeRestUnitRepositoryConfig
      }

      "can be loaded for Companies House" in new Fixture {
        (restUnitRepositoryConfigLoader.load _).expects(TheConfig, "api.admin.data.ch").returning(SomeRestUnitRepositoryConfig)

        RestAdminDataRepositoryConfigLoader.companiesHouse(restUnitRepositoryConfigLoader, TheConfig) shouldBe SomeRestUnitRepositoryConfig
      }
    }

    "cannot be loaded when it contains an invalid configuration" in new Fixture {
      (restUnitRepositoryConfigLoader.load _).expects(TheConfig, *).throwing(new Missing("key"))

      a[ConfigException] shouldBe thrownBy {
        RestAdminDataRepositoryConfigLoader.vat(restUnitRepositoryConfigLoader, TheConfig)
      }
    }
  }
}
