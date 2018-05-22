package config

import com.typesafe.config.{ ConfigException, ConfigFactory }
import org.scalatest.{ FreeSpec, Matchers }
import repository.sbrctrl.SbrCtrlUnitRepositoryConfig
import utils.url.BaseUrl

class SbrCtrlUnitRepositoryConfigLoaderSpec extends FreeSpec with Matchers {

  private trait Fixture {
    val Missing = "missing"
    val SampleConfiguration =
      """
        |api {
        |  sbr {
        |    control {
        |      protocol = "http"
        |      host = "localhost"
        |      port = 4567
        |    }
        |  }
        |}""".stripMargin
    val config = ConfigFactory.parseString(SampleConfiguration)
  }

  "The config for an SBR Control unit repository" - {
    "can be successfully loaded when valid" in new Fixture {
      SbrCtrlUnitRepositoryConfigLoader.load(config) shouldBe SbrCtrlUnitRepositoryConfig(
        BaseUrl(protocol = "http", host = "localhost", port = 4567)
      )
    }

    "cannot be loaded" - {
      "when protocol" - {
        "is missing" in new Fixture {
          val badConfig = ConfigFactory.parseString(SampleConfiguration.replace("protocol", Missing))

          a[ConfigException] shouldBe thrownBy {
            SbrCtrlUnitRepositoryConfigLoader.load(badConfig)
          }
        }
      }

      "when host" - {
        "is missing" in new Fixture {
          val badConfig = ConfigFactory.parseString(SampleConfiguration.replace("host", Missing))

          a[ConfigException] shouldBe thrownBy {
            SbrCtrlUnitRepositoryConfigLoader.load(badConfig)
          }
        }
      }

      "when port" - {
        "is missing" in new Fixture {
          val badConfig = ConfigFactory.parseString(SampleConfiguration.replace("port", Missing))

          a[ConfigException] shouldBe thrownBy {
            SbrCtrlUnitRepositoryConfigLoader.load(badConfig)
          }
        }

        "is non-numeric" in new Fixture {
          val badConfig = ConfigFactory.parseString(SampleConfiguration.replace("4567", "eightyeighty"))

          a[ConfigException] shouldBe thrownBy {
            SbrCtrlUnitRepositoryConfigLoader.load(badConfig)
          }
        }
      }
    }
  }
}
