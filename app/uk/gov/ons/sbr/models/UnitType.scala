package uk.gov.ons.sbr.models

import play.api.libs.json._
import play.api.libs.json.Reads.JsStringReads
import utils.JsResultSupport

sealed trait UnitType

object UnitType {
  case object Enterprise extends UnitType
  case object LegalUnit extends UnitType
  case object LocalUnit extends UnitType
  case object ValueAddedTax extends UnitType
  case object PayAsYouEarn extends UnitType
  case object CompaniesHouse extends UnitType

  private object Acronym {
    val Enterprise = "ENT"
    val LegalUnit = "LEU"
    val LocalUnit = "LOU"
    val ValueAddedTax = "VAT"
    val PayeAsYouEarn = "PAYE"
    val CompaniesHouse = "CH"
  }

  def toAcronym(unitType: UnitType): String =
    unitType match {
      case Enterprise => Acronym.Enterprise
      case LegalUnit => Acronym.LegalUnit
      case LocalUnit => Acronym.LocalUnit
      case ValueAddedTax => Acronym.ValueAddedTax
      case PayAsYouEarn => Acronym.PayeAsYouEarn
      case CompaniesHouse => Acronym.CompaniesHouse
    }

  def fromAcronym(acronym: String): Option[UnitType] =
    acronym match {
      case Acronym.Enterprise => Some(UnitType.Enterprise)
      case Acronym.LegalUnit => Some(UnitType.LegalUnit)
      case Acronym.LocalUnit => Some(UnitType.LocalUnit)
      case Acronym.ValueAddedTax => Some(UnitType.ValueAddedTax)
      case Acronym.PayeAsYouEarn => Some(UnitType.PayAsYouEarn)
      case Acronym.CompaniesHouse => Some(UnitType.CompaniesHouse)
      case _ => None
    }

  /*
   * Reads from / writes to a simple Json string (as the unit type acronym).
   */
  object JsonFormat extends Format[UnitType] {
    override def reads(json: JsValue): JsResult[UnitType] =
      JsStringReads.reads(json).flatMap { acronymJsStr =>
        JsResultSupport.fromOption(fromAcronym(acronymJsStr.value))
      }

    override def writes(unitType: UnitType): JsValue =
      JsString(toAcronym(unitType))
  }
}