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
    val SomeParents: Option[Map[UnitType, UnitId]] = Some(Map(
      UnitType.Enterprise -> UnitId("9876543210")
    ))
    val SomeChildren = Some(Map(
      UnitId("123456789") -> UnitType.LocalUnit,
      UnitId("87654321") -> UnitType.LegalUnit
    ))
    val SomeUnitLinks = UnitLinks(SomeUnitId, SomeUnitType, SomePeriod, SomeParents, SomeChildren)
    val SomeVariablesJson = SampleEnterprise.asJson(Ern(SomeUnitId.value))

    def jsonStringFor(lu: LinkedUnit): String =
      withObject(
        string("id", lu.id.value),
        string("unitType", UnitType.toAcronym(lu.unitType)),
        string("period", Period.asString(lu.period)),
        lu.parents.map { unitIdsByUnitType =>
          withObject("parents", unitIdsByUnitType.toSeq.map {
            case (unitType, unitId) => string(UnitType.toAcronym(unitType), unitId.value)
          }: _*)
        },
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
      LinkedUnit.wrap(SomeUnitLinks, SomeVariablesJson) shouldBe LinkedUnit(
        SomeUnitId,
        SomeUnitType,
        SomePeriod,
        SomeParents,
        SomeChildren,
        SomeVariablesJson
      )
    }

    "can be written to Json" - {
      "when it has both parent and child links" in new Fixture {
        val linkedUnit = LinkedUnit.wrap(SomeUnitLinks, SomeVariablesJson)

        LinkedUnit.writes.writes(linkedUnit) shouldBe Json.parse(jsonStringFor(linkedUnit))
      }

      "when it has only child links" in new Fixture {
        val linkedUnit = LinkedUnit.wrap(SomeUnitLinks.copy(parents = None), SomeVariablesJson)

        LinkedUnit.writes.writes(linkedUnit) shouldBe Json.parse(jsonStringFor(linkedUnit))
      }

      "when it has empty child links" in new Fixture {
        val linkedUnit = LinkedUnit.wrap(SomeUnitLinks.copy(children = Some(Map.empty)), SomeVariablesJson)

        LinkedUnit.writes.writes(linkedUnit) shouldBe Json.parse(jsonStringFor(linkedUnit))
      }

      "when it has only parent links" in new Fixture {
        val linkedUnit = LinkedUnit.wrap(SomeUnitLinks.copy(children = None), SomeVariablesJson)

        LinkedUnit.writes.writes(linkedUnit) shouldBe Json.parse(jsonStringFor(linkedUnit))
      }

      "when it has empty parent links" in new Fixture {
        val linkedUnit = LinkedUnit.wrap(SomeUnitLinks.copy(parents = Some(Map.empty)), SomeVariablesJson)

        LinkedUnit.writes.writes(linkedUnit) shouldBe Json.parse(jsonStringFor(linkedUnit))
      }

      "when it has no links" in new Fixture {
        val linkedUnit = LinkedUnit.wrap(SomeUnitLinks.copy(parents = None, children = None), SomeVariablesJson)

        LinkedUnit.writes.writes(linkedUnit) shouldBe Json.parse(jsonStringFor(linkedUnit))
      }
    }
  }
}
