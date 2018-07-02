package uk.gov.ons.sbr.models

import java.time.Month.{ FEBRUARY, MARCH }
import java.time.YearMonth
import java.time.format.DateTimeParseException

import org.scalatest.{ FreeSpec, Matchers }
import play.api.libs.json.{ JsError, JsNumber, JsString, JsSuccess }

import scala.util.{ Failure, Success }

class PeriodSpec extends FreeSpec with Matchers {

  "A Period" - {
    "can parse a valid string representation (uuuuMM format)" in {
      Period.parseString("201803") shouldBe Success(Period(YearMonth.of(2018, MARCH)))
    }

    "does not parse an invalid string representation" - {
      "that is non-numeric" in {
        Period.parseString("2018-3") shouldBe a[Failure[_]]
      }

      "has too few digits" in {
        Period.parseString("2018") shouldBe a[Failure[_]]
      }

      "has too many digits" in {
        Period.parseString("20180328") shouldBe a[Failure[_]]
      }

      "has an invalid month" in {
        Period.parseString("201813") shouldBe a[Failure[_]]
      }
    }

    "can be created from a valid string representation (uuuuMM format)" in {
      Period.fromString("201802") shouldBe Period(YearMonth.of(2018, FEBRUARY))
    }

    "cannot be created from an invalid string representation" in {
      a[DateTimeParseException] should be thrownBy {
        Period.fromString("2018-02")
      }
    }

    "can be created from a year and month" in {
      Period.fromYearMonth(2018, FEBRUARY) shouldBe Period(YearMonth.of(2018, FEBRUARY))
    }

    "can be represented as a string" in {
      Period.asString(Period.fromYearMonth(2018, FEBRUARY)) shouldBe "201802"
    }

    "has a helpful debug representation" in {
      Period.fromYearMonth(2018, FEBRUARY).toString shouldBe "Period(201802)"
    }
  }

  "A Period JSON Format" - {
    "writes a period as a Json string" in {
      Period.JsonFormat.writes(Period.fromYearMonth(2018, FEBRUARY)) shouldBe JsString("201802")
    }

    "reads a period from Json" - {
      "when a valid string" in {
        Period.JsonFormat.reads(JsString("201802")) shouldBe JsSuccess(Period.fromYearMonth(2018, FEBRUARY))
      }

      "failing when a string containing an invalid representation" - {
        "that is non-numeric" in {
          Period.JsonFormat.reads(JsString("2018-3")) shouldBe a[JsError]
        }

        "has too few digits" in {
          Period.JsonFormat.reads(JsString("2018")) shouldBe a[JsError]
        }

        "has too many digits" in {
          Period.JsonFormat.reads(JsString("20180328")) shouldBe a[JsError]
        }

        "has an invalid month" in {
          Period.JsonFormat.reads(JsString("201813")) shouldBe a[JsError]
        }
      }

      "failing when not a Json string" in {
        Period.JsonFormat.reads(JsNumber(42)) shouldBe a[JsError]
      }
    }
  }
}
