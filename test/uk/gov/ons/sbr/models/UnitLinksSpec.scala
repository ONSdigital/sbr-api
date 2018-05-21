package uk.gov.ons.sbr.models

import java.time.Month.FEBRUARY

import org.scalatest.{ FreeSpec, Matchers }
import play.api.libs.json.JsResultException
import play.api.libs.json.Json.parse
import support.JsonString.{ optionalString, string, withObject }

class UnitLinksSpec extends FreeSpec with Matchers {

  private trait Fixture {
    implicit val readsUnitLinks = UnitLinks.reads
    val Id = "1234567890"
    val SampleParents = Map("ENT" -> "9988776655")
    val SampleChildren = Map("123456789" -> "LOU", "12345678" -> "LEU")
    val SampleUnitLinksDefinition = UnitLinksJsonResponseDefinition(
      withId = Some(Id),
      withUnitType = Some("ENT"),
      withPeriod = Some("201802"),
      withParents = Some(SampleParents),
      withChildren = Some(SampleChildren)
    )

    case class UnitLinksJsonResponseDefinition(
      withId: Option[String],
      withUnitType: Option[String],
      withPeriod: Option[String],
      withParents: Option[Map[String, String]],
      withChildren: Option[Map[String, String]]
    )

    def unitLinksJsonResponse(definition: UnitLinksJsonResponseDefinition): String = {
      val optParentIdByUnitType = definition.withParents.map { idsByUnitType =>
        idsByUnitType.toSeq.map {
          case (unitType, id) =>
            string(unitType, id)
        }
      }
      val optChildUnitTypeById = definition.withChildren.map { unitTypesById =>
        unitTypesById.toSeq.map {
          case (id, unitType) =>
            string(id, unitType)
        }
      }

      withObject(
        optionalString("id", definition.withId),
        optParentIdByUnitType.map { parents =>
          withObject("parents", parents: _*)
        },
        optChildUnitTypeById.map { children =>
          withObject("children", children: _*)
        },
        optionalString("unitType", definition.withUnitType),
        optionalString("period", definition.withPeriod)
      )
    }
  }

  "Unit Links" - {
    "can be read from a Json representation of unit links" - {
      "containing no parent and no children" in new Fixture {
        val unitLinks = parse(unitLinksJsonResponse(SampleUnitLinksDefinition.copy(withParents = None, withChildren = None))).as[UnitLinks]

        unitLinks shouldBe UnitLinks(
          id = UnitId(Id),
          unitType = UnitType.Enterprise,
          period = Period.fromYearMonth(2018, FEBRUARY),
          parents = None,
          children = None
        )
      }

      "containing empty parents and no children" in new Fixture {
        val emptyParentsJson = unitLinksJsonResponse(SampleUnitLinksDefinition.copy(withParents = Some(Map.empty), withChildren = None))

        parse(emptyParentsJson).as[UnitLinks] shouldBe UnitLinks(
          id = UnitId(Id),
          unitType = UnitType.Enterprise,
          period = Period.fromYearMonth(2018, FEBRUARY),
          parents = None,
          children = None
        )
      }

      "containing no parent and empty children" in new Fixture {
        val emptyChildrenJson = unitLinksJsonResponse(SampleUnitLinksDefinition.copy(withParents = None, withChildren = Some(Map.empty)))

        parse(emptyChildrenJson).as[UnitLinks] shouldBe UnitLinks(
          id = UnitId(Id),
          unitType = UnitType.Enterprise,
          period = Period.fromYearMonth(2018, FEBRUARY),
          parents = None,
          children = None
        )
      }

      "containing both parent and child links" in new Fixture {
        val unitLinks = parse(unitLinksJsonResponse(SampleUnitLinksDefinition)).as[UnitLinks]

        unitLinks shouldBe UnitLinks(
          id = UnitId(Id),
          unitType = UnitType.Enterprise,
          period = Period.fromYearMonth(2018, FEBRUARY),
          parents = Some(Map(
            UnitType.Enterprise -> UnitId("9988776655")
          )),
          children = Some(Map(
            UnitId("123456789") -> UnitType.LocalUnit,
            UnitId("12345678") -> UnitType.LegalUnit
          ))
        )
      }

      "containing only parent links" in new Fixture {
        val unitLinks = parse(unitLinksJsonResponse(SampleUnitLinksDefinition.copy(withChildren = None))).as[UnitLinks]

        unitLinks shouldBe UnitLinks(
          id = UnitId(Id),
          unitType = UnitType.Enterprise,
          period = Period.fromYearMonth(2018, FEBRUARY),
          parents = Some(Map(
            UnitType.Enterprise -> UnitId("9988776655")
          )),
          children = None
        )
      }

      "containing only child links" in new Fixture {
        val unitLinks = parse(unitLinksJsonResponse(SampleUnitLinksDefinition.copy(withParents = None))).as[UnitLinks]

        unitLinks shouldBe UnitLinks(
          id = UnitId(Id),
          unitType = UnitType.Enterprise,
          period = Period.fromYearMonth(2018, FEBRUARY),
          parents = None,
          children = Some(Map(
            UnitId("123456789") -> UnitType.LocalUnit,
            UnitId("12345678") -> UnitType.LegalUnit
          ))
        )
      }
    }

    "cannot be read from the Json representation of unit links when" - {
      "the unit Id" - {
        "is missing" in new Fixture {
          val definition = SampleUnitLinksDefinition.copy(withId = None)

          a[JsResultException] should be thrownBy {
            parse(unitLinksJsonResponse(definition)).as[UnitLinks]
          }
        }

        "is not a string value" in new Fixture {
          val baseJson = unitLinksJsonResponse(SampleUnitLinksDefinition.copy(withId = None, withParents = None, withChildren = None))
          val jsonWithNumericId = baseJson.replace("}", s""","id":123}""")

          a[JsResultException] should be thrownBy {
            parse(jsonWithNumericId).as[UnitLinks]
          }
        }
      }

      "the unit Type" - {
        "is missing" in new Fixture {
          val definition = SampleUnitLinksDefinition.copy(withUnitType = None)

          a[JsResultException] should be thrownBy {
            parse(unitLinksJsonResponse(definition)).as[UnitLinks]
          }
        }

        "is unrecognised" in new Fixture {
          val definition = SampleUnitLinksDefinition.copy(withUnitType = Some("UNKNOWN"))

          a[JsResultException] should be thrownBy {
            parse(unitLinksJsonResponse(definition)).as[UnitLinks]
          }
        }

        "is not a string value" in new Fixture {
          val baseJson = unitLinksJsonResponse(SampleUnitLinksDefinition.copy(withUnitType = None, withParents = None, withChildren = None))
          val jsonWithObjectType = baseJson.replace("}", s""","unitType":{"value":"ENT"}}""")

          a[JsResultException] should be thrownBy {
            parse(jsonWithObjectType).as[UnitLinks]
          }
        }
      }

      "the period" - {
        "is missing" in new Fixture {
          val definition = SampleUnitLinksDefinition.copy(withPeriod = None)

          a[JsResultException] should be thrownBy {
            parse(unitLinksJsonResponse(definition)).as[UnitLinks]
          }
        }

        "has non-numeric content" in new Fixture {
          val definition = SampleUnitLinksDefinition.copy(withPeriod = Some("2018/3"))

          a[JsResultException] should be thrownBy {
            parse(unitLinksJsonResponse(definition)).as[UnitLinks]
          }
        }

        "has less than 6 digits" in new Fixture {
          val definition = SampleUnitLinksDefinition.copy(withPeriod = Some("2018"))

          a[JsResultException] should be thrownBy {
            parse(unitLinksJsonResponse(definition)).as[UnitLinks]
          }
        }

        "has a month value of 00" in new Fixture {
          val definition = SampleUnitLinksDefinition.copy(withPeriod = Some("201800"))

          a[JsResultException] should be thrownBy {
            parse(unitLinksJsonResponse(definition)).as[UnitLinks]
          }
        }

        "has a month value greater than 12" in new Fixture {
          val definition = SampleUnitLinksDefinition.copy(withPeriod = Some("201813"))

          a[JsResultException] should be thrownBy {
            parse(unitLinksJsonResponse(definition)).as[UnitLinks]
          }
        }

        "is not a string value" in new Fixture {
          val baseJson = unitLinksJsonResponse(SampleUnitLinksDefinition.copy(withPeriod = None, withParents = None, withChildren = None))
          val jsonWithNumericPeriod = baseJson.replace("}", s""","period":201803}""")

          a[JsResultException] should be thrownBy {
            parse(jsonWithNumericPeriod).as[UnitLinks]
          }
        }
      }

      "a child" - {
        "has a unit Type" - {
          "that is not a string value" in new Fixture {
            val baseJson = unitLinksJsonResponse(SampleUnitLinksDefinition.copy(withParents = None, withChildren = None))
            val jsonWithNonStringChildType = baseJson.replace("}", s""","children":{"childid":{}}}""")

            a[JsResultException] should be thrownBy {
              parse(jsonWithNonStringChildType).as[UnitLinks]
            }
          }

          "that is unrecognised" in new Fixture {
            val baseJson = unitLinksJsonResponse(SampleUnitLinksDefinition.copy(withParents = None, withChildren = None))
            val jsonWithUnrecognisedChildUnitType = baseJson.replace("}", s""","children":{"childid":"UNKNOWN"}}""")

            a[JsResultException] should be thrownBy {
              parse(jsonWithUnrecognisedChildUnitType).as[UnitLinks]
            }
          }
        }
      }

      "a parent" - {
        "has a unit Type" - {
          "that is unrecognised" in new Fixture {
            val baseJson = unitLinksJsonResponse(SampleUnitLinksDefinition.copy(withParents = None, withChildren = None))
            val jsonWithUnrecognisedParentType = baseJson.replace("}", s""","parents":{"UNKNOWN":"parent-id"}}""")

            a[JsResultException] should be thrownBy {
              parse(jsonWithUnrecognisedParentType).as[UnitLinks]
            }
          }
        }

        "has a unit Id" - {
          "that is not a string value" in new Fixture {
            val baseJson = unitLinksJsonResponse(SampleUnitLinksDefinition.copy(withParents = None, withChildren = None))
            val jsonWithNumericParentId = baseJson.replace("}", s""","parents":{"ENT":9876543210}}""")

            a[JsResultException] should be thrownBy {
              parse(jsonWithNumericParentId).as[UnitLinks]
            }
          }
        }
      }
    }
  }
}
