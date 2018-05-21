package config

import com.typesafe.config.{ ConfigException, ConfigFactory }
import org.scalatest.{ FreeSpec, Matchers }
import utils.url.BaseUrl

class BaseUrlConfigLoaderSpec extends FreeSpec with Matchers {

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
    val BaseUrlPath = "api.sbr.control"
    val config = ConfigFactory.parseString(SampleConfiguration)
  }

  "The config for a base URL" - {
    "can be successfully loaded when valid and existing at the specified path" in new Fixture {
      BaseUrlConfigLoader.load(config, BaseUrlPath) shouldBe
        BaseUrl(protocol = "http", host = "localhost", port = 4567)
    }

    "cannot be loaded" - {
      "when it does not exist at the specified path" in new Fixture {
        a[ConfigException] shouldBe thrownBy {
          BaseUrlConfigLoader.load(config, "api.sbr")
        }
      }

      "when the config is invalid" - {
        "because the protocol" - {
          "is missing" in new Fixture {
            val badConfig = ConfigFactory.parseString(SampleConfiguration.replace("protocol", Missing))

            a[ConfigException] shouldBe thrownBy {
              BaseUrlConfigLoader.load(badConfig, BaseUrlPath)
            }
          }
        }

        "because the host" - {
          "is missing" in new Fixture {
            val badConfig = ConfigFactory.parseString(SampleConfiguration.replace("host", Missing))

            a[ConfigException] shouldBe thrownBy {
              BaseUrlConfigLoader.load(badConfig, BaseUrlPath)
            }
          }
        }

        "because the port" - {
          "is missing" in new Fixture {
            val badConfig = ConfigFactory.parseString(SampleConfiguration.replace("port", Missing))

            a[ConfigException] shouldBe thrownBy {
              BaseUrlConfigLoader.load(badConfig, BaseUrlPath)
            }
          }

          "is non-numeric" in new Fixture {
            val badConfig = ConfigFactory.parseString(SampleConfiguration.replace("4567", "eightyeighty"))

            a[ConfigException] shouldBe thrownBy {
              BaseUrlConfigLoader.load(badConfig, BaseUrlPath)
            }
          }
        }
      }
    }
  }
}