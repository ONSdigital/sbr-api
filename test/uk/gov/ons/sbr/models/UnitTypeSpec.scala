package uk.gov.ons.sbr.models

import org.scalatest.{ FreeSpec, Matchers }
import play.api.libs.json.{ JsError, JsNumber, JsString, JsSuccess }

class UnitTypeSpec extends FreeSpec with Matchers {

  "A UnitType" - {
    "can be represented as an acronym" - {
      "when an Enterprise" in {
        UnitType.toAcronym(UnitType.Enterprise) shouldBe "ENT"
      }

      "when a Legal Unit" in {
        UnitType.toAcronym(UnitType.LegalUnit) shouldBe "LEU"
      }

      "when a Local Unit" in {
        UnitType.toAcronym(UnitType.LocalUnit) shouldBe "LOU"
      }

      "when a Reporting Unit" in {
        UnitType.toAcronym(UnitType.ReportingUnit) shouldBe "REU"
      }

      "when a Value Added Tax" in {
        UnitType.toAcronym(UnitType.ValueAddedTax) shouldBe "VAT"
      }

      "when a Pay As You Earn" in {
        UnitType.toAcronym(UnitType.PayAsYouEarn) shouldBe "PAYE"
      }

      "when a Companies House" in {
        UnitType.toAcronym(UnitType.CompaniesHouse) shouldBe "CH"
      }
    }

    "can be identified from a known acronym" - {
      "when an Enterprise" in {
        UnitType.fromAcronym("ENT") shouldBe Some(UnitType.Enterprise)
      }

      "when a Legal Unit" in {
        UnitType.fromAcronym("LEU") shouldBe Some(UnitType.LegalUnit)
      }

      "when a Local Unit" in {
        UnitType.fromAcronym("LOU") shouldBe Some(UnitType.LocalUnit)
      }

      "when a Reporting Unit" in {
        UnitType.fromAcronym("REU") shouldBe Some(UnitType.ReportingUnit)
      }

      "when a Value Added Tax" in {
        UnitType.fromAcronym("VAT") shouldBe Some(UnitType.ValueAddedTax)
      }

      "when a Pay As You Earn" in {
        UnitType.fromAcronym("PAYE") shouldBe Some(UnitType.PayAsYouEarn)
      }

      "when a Companies House" in {
        UnitType.fromAcronym("CH") shouldBe Some(UnitType.CompaniesHouse)
      }
    }

    "cannot be identified from an unknown acronym" in {
      UnitType.fromAcronym("UNKNOWN") shouldBe None
    }
  }

  "A UnitType JSON Format" - {
    "writes a UnitType as a Json string (using the acronym value)" - {
      "when an Enterprise" in {
        UnitType.JsonFormat.writes(UnitType.Enterprise) shouldBe JsString("ENT")
      }

      "when a Legal Unit" in {
        UnitType.JsonFormat.writes(UnitType.LegalUnit) shouldBe JsString("LEU")
      }

      "when a Local Unit" in {
        UnitType.JsonFormat.writes(UnitType.LocalUnit) shouldBe JsString("LOU")
      }

      "when a Reporting Unit" in {
        UnitType.JsonFormat.writes(UnitType.ReportingUnit) shouldBe JsString("REU")
      }

      "when a Value Added Tax" in {
        UnitType.JsonFormat.writes(UnitType.ValueAddedTax) shouldBe JsString("VAT")
      }

      "when a Pay As You Earn" in {
        UnitType.JsonFormat.writes(UnitType.PayAsYouEarn) shouldBe JsString("PAYE")
      }

      "when a Companies House" in {
        UnitType.JsonFormat.writes(UnitType.CompaniesHouse) shouldBe JsString("CH")
      }
    }

    "reads a UnitType from a Json string" - {
      "when an Enterprise" in {
        UnitType.JsonFormat.reads(JsString("ENT")) shouldBe JsSuccess(UnitType.Enterprise)
      }

      "when a Legal Unit" in {
        UnitType.JsonFormat.reads(JsString("LEU")) shouldBe JsSuccess(UnitType.LegalUnit)
      }

      "when a Local Unit" in {
        UnitType.JsonFormat.reads(JsString("LOU")) shouldBe JsSuccess(UnitType.LocalUnit)
      }

      "when a Reporting Unit" in {
        UnitType.JsonFormat.reads(JsString("REU")) shouldBe JsSuccess(UnitType.ReportingUnit)
      }

      "when a Value Added Tax" in {
        UnitType.JsonFormat.reads(JsString("VAT")) shouldBe JsSuccess(UnitType.ValueAddedTax)
      }

      "when a Pay As You Earn" in {
        UnitType.JsonFormat.reads(JsString("PAYE")) shouldBe JsSuccess(UnitType.PayAsYouEarn)
      }

      "when a Companies House" in {
        UnitType.JsonFormat.reads(JsString("CH")) shouldBe JsSuccess(UnitType.CompaniesHouse)
      }
    }

    "fails to read a Unit Type from Json" - {
      "when a Json string containing a value that is not a known acronym" in {
        UnitType.JsonFormat.reads(JsString("UNKNOWN")) shouldBe a[JsError]
      }

      "when not a Json string" in {
        UnitType.JsonFormat.reads(JsNumber(42)) shouldBe a[JsError]
      }
    }
  }
}
