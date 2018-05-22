package uk.gov.ons.sbr.models

import java.time.Month.MARCH

import org.scalatest.{ FreeSpec, Matchers }
import play.api.libs.json.Json
import support.JsonString.{ string, withObject }
import support.sample.SampleEnterprise

class LinkedUnitSpec extends FreeSpec with Matchers {

  private trait Fixture {
    val SomeUnitId = UnitId("1234567890")
    val SomeUnitType = UnitType.Enterprise
    val SomePeriod = Period.fromYearMonth(2018, MARCH)
    val SomeEnterpriseChildren = Some(Map(
      UnitId("123456789") -> UnitType.LocalUnit,
      UnitId("87654321") -> UnitType.LegalUnit
    ))
    val SomeUnitLinks = UnitLinks(SomeUnitId, SomeUnitType, SomePeriod, SomeEnterpriseChildren)
    val SomeEnterpriseJson = SampleEnterprise.asJson(Ern(SomeUnitId.value))

    def jsonStringFor(lu: LinkedUnit): String =
      withObject(
        string("id", lu.id.value),
        string("unitType", UnitType.toAcronym(lu.unitType)),
        string("period", Period.asString(lu.period)),
        lu.children.map { unitTypeByUnitId =>
          withObject("children", unitTypeByUnitId.toSeq.map {
            case (unitId, unitType) => string(unitId.value, UnitType.toAcronym(unitType))
          }: _*)
        },
        Some(s""""vars":${SampleEnterprise(Ern(lu.id.value))}""")
      )
  }

  "A LinkedUnit" - {
    "can be created by wrapping a unit with its associated unit links" in new Fixture {
      LinkedUnit.wrap(SomeUnitLinks, SomeEnterpriseJson) shouldBe LinkedUnit(
        SomeUnitId,
        SomeUnitType,
        SomePeriod,
        SomeEnterpriseChildren,
        SomeEnterpriseJson
      )
    }

    "can be written to Json" - {
      "when it has child links" in new Fixture {
        val linkedUnit = LinkedUnit.wrap(SomeUnitLinks, SomeEnterpriseJson)

        LinkedUnit.writes.writes(linkedUnit) shouldBe Json.parse(jsonStringFor(linkedUnit))
      }

      "when it has empty child links" in new Fixture {
        val linkedUnit = LinkedUnit.wrap(SomeUnitLinks.copy(children = Some(Map.empty)), SomeEnterpriseJson)

        LinkedUnit.writes.writes(linkedUnit) shouldBe Json.parse(jsonStringFor(linkedUnit))
      }

      "when it has no child links" in new Fixture {
        val linkedUnit = LinkedUnit.wrap(SomeUnitLinks.copy(children = None), SomeEnterpriseJson)

        LinkedUnit.writes.writes(linkedUnit) shouldBe Json.parse(jsonStringFor(linkedUnit))
      }
    }
  }
}
