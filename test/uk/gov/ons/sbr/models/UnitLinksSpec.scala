package uk.gov.ons.sbr.models

import java.time.Month.FEBRUARY

import org.scalatest.{ FreeSpec, Matchers }
import play.api.libs.json.JsResultException
import play.api.libs.json.Json.parse
import support.JsonString.{ optionalString, string, withObject }

class UnitLinksSpec extends FreeSpec with Matchers {

  private trait Fixture {
    implicit val readsUnitLinks2 = UnitLinks.reads
    val SampleEnterpriseChildren = Map("123456789" -> "LOU", "12345678" -> "LEU")
    val SampleEnterpriseUnitLinksDefinition = UnitLinksJsonResponseDefinition(
      withId = Some("1234567890"),
      withUnitType = Some("ENT"),
      withPeriod = Some("201802"),
      withChildren = Some(SampleEnterpriseChildren)
    )

    case class UnitLinksJsonResponseDefinition(
      withId: Option[String],
      withUnitType: Option[String],
      withPeriod: Option[String],
      withChildren: Option[Map[String, String]]
    )

    def unitLinksJsonResponse(definition: UnitLinksJsonResponseDefinition): String = {
      val optChildUnitTypeById = definition.withChildren.map { unitTypesById =>
        unitTypesById.toSeq.map {
          case (id, unitType) =>
            string(id, unitType)
        }
      }

      withObject(
        optionalString("id", definition.withId),
        optChildUnitTypeById.map { children =>
          withObject("children", children: _*)
        },
        optionalString("unitType", definition.withUnitType),
        optionalString("period", definition.withPeriod)
      )
    }
  }

  "Unit Links" - {
    "can be read from a Json representation of unit links containing no parent and no children" in new Fixture {
      val unitLinks = parse(unitLinksJsonResponse(SampleEnterpriseUnitLinksDefinition.copy(withChildren = None))).as[UnitLinks]

      unitLinks shouldBe UnitLinks(
        id = UnitId("1234567890"),
        unitType = UnitType.Enterprise,
        period = Period.fromYearMonth(2018, FEBRUARY),
        children = None
      )
    }

    "can be read from a Json representation of unit links containing no parent and empty children" in new Fixture {
      val noChildrenJson = unitLinksJsonResponse(SampleEnterpriseUnitLinksDefinition.copy(withChildren = None))
      val emptyChildrenJson = noChildrenJson.replace("}", s""","children":{}}""")

      parse(emptyChildrenJson).as[UnitLinks] shouldBe UnitLinks(
        id = UnitId("1234567890"),
        unitType = UnitType.Enterprise,
        period = Period.fromYearMonth(2018, FEBRUARY),
        children = None
      )
    }

    "can be read from a Json representation of an enterprise with child links" in new Fixture {
      val unitLinks = parse(unitLinksJsonResponse(SampleEnterpriseUnitLinksDefinition)).as[UnitLinks]

      unitLinks shouldBe UnitLinks(
        id = UnitId("1234567890"),
        unitType = UnitType.Enterprise,
        period = Period.fromYearMonth(2018, FEBRUARY),
        children = Some(Map(
          UnitId("123456789") -> UnitType.LocalUnit,
          UnitId("12345678") -> UnitType.LegalUnit
        ))
      )
    }

    "cannot be read from the Json representation of enterprise links when" - {
      "the unit Id" - {
        "is missing" in new Fixture {
          val definition = SampleEnterpriseUnitLinksDefinition.copy(withId = None)

          a[JsResultException] should be thrownBy {
            parse(unitLinksJsonResponse(definition)).as[UnitLinks]
          }
        }

        "is not a string value" in new Fixture {
          val baseJson = unitLinksJsonResponse(SampleEnterpriseUnitLinksDefinition.copy(withId = None, withChildren = None))
          val jsonWithNumericId = baseJson.replace("}", s""","id":123}""")

          a[JsResultException] should be thrownBy {
            parse(jsonWithNumericId).as[UnitLinks]
          }
        }
      }

      "the unit Type" - {
        "is missing" in new Fixture {
          val definition = SampleEnterpriseUnitLinksDefinition.copy(withUnitType = None)

          a[JsResultException] should be thrownBy {
            parse(unitLinksJsonResponse(definition)).as[UnitLinks]
          }
        }

        "is unrecognised" in new Fixture {
          val definition = SampleEnterpriseUnitLinksDefinition.copy(withUnitType = Some("UNKNOWN"))

          a[JsResultException] should be thrownBy {
            parse(unitLinksJsonResponse(definition)).as[UnitLinks]
          }
        }

        "is not a string value" in new Fixture {
          val baseJson = unitLinksJsonResponse(SampleEnterpriseUnitLinksDefinition.copy(withUnitType = None, withChildren = None))
          val jsonWithObjectType = baseJson.replace("}", s""","unitType":{"value":"ENT"}}""")

          a[JsResultException] should be thrownBy {
            parse(jsonWithObjectType).as[UnitLinks]
          }
        }
      }

      "the period" - {
        "is missing" in new Fixture {
          val definition = SampleEnterpriseUnitLinksDefinition.copy(withPeriod = None)

          a[JsResultException] should be thrownBy {
            parse(unitLinksJsonResponse(definition)).as[UnitLinks]
          }
        }

        "has non-numeric content" in new Fixture {
          val definition = SampleEnterpriseUnitLinksDefinition.copy(withPeriod = Some("2018/3"))

          a[JsResultException] should be thrownBy {
            parse(unitLinksJsonResponse(definition)).as[UnitLinks]
          }
        }

        "has less than 6 digits" in new Fixture {
          val definition = SampleEnterpriseUnitLinksDefinition.copy(withPeriod = Some("2018"))

          a[JsResultException] should be thrownBy {
            parse(unitLinksJsonResponse(definition)).as[UnitLinks]
          }
        }

        "has a month value of 00" in new Fixture {
          val definition = SampleEnterpriseUnitLinksDefinition.copy(withPeriod = Some("201800"))

          a[JsResultException] should be thrownBy {
            parse(unitLinksJsonResponse(definition)).as[UnitLinks]
          }
        }

        "has a month value greater than 12" in new Fixture {
          val definition = SampleEnterpriseUnitLinksDefinition.copy(withPeriod = Some("201813"))

          a[JsResultException] should be thrownBy {
            parse(unitLinksJsonResponse(definition)).as[UnitLinks]
          }
        }

        "is not a string value" in new Fixture {
          val baseJson = unitLinksJsonResponse(SampleEnterpriseUnitLinksDefinition.copy(withPeriod = None, withChildren = None))
          val jsonWithNumericPeriod = baseJson.replace("}", s""","period":201803}""")

          a[JsResultException] should be thrownBy {
            parse(jsonWithNumericPeriod).as[UnitLinks]
          }
        }
      }

      "a child" - {
        "has a unit Type" - {
          "that is not a string value" in new Fixture {
            val baseJson = unitLinksJsonResponse(SampleEnterpriseUnitLinksDefinition.copy(withChildren = None))
            val jsonWithNonStringChildType = baseJson.replace("}", s""","children":{"childid":{}}}""")

            a[JsResultException] should be thrownBy {
              parse(jsonWithNonStringChildType).as[UnitLinks]
            }
          }

          "that is unrecognised" in new Fixture {
            val baseJson = unitLinksJsonResponse(SampleEnterpriseUnitLinksDefinition.copy(withChildren = None))
            val jsonWithUnrecognisedChildUnitType = baseJson.replace("}", s""","children":{"childid":"UNKNOWN"}}""")

            a[JsResultException] should be thrownBy {
              parse(jsonWithUnrecognisedChildUnitType).as[UnitLinks]
            }
          }
        }
      }
    }
  }
}
