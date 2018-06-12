package uk.gov.ons.sbr.models

import java.time.Month.APRIL

import org.scalatest.{ FreeSpec, Matchers }
import play.api.libs.json.Json
import support.sample.SampleVat

class AdminDataSpec extends FreeSpec with Matchers {

  private trait Fixture {
    val SomeVatRef = VatRef("987654321012")
    val SampleJsonStr =
      s"""|{"period":"201804",
          | "id":"${SomeVatRef.value}",
          | "variables":${SampleVat(SomeVatRef)}
          |}""".stripMargin
  }

  "Admin Data" - {
    "can be read from a Json representation" in new Fixture {
      Json.parse(SampleJsonStr).as[AdminData](AdminData.reads) shouldBe AdminData(
        id = UnitId(SomeVatRef.value),
        period = Period.fromYearMonth(2018, APRIL),
        variables = SampleVat.asJson(SomeVatRef)
      )
    }
  }
}
