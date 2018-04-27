package uk.gov.ons.sbr.models

case class Ern(value: String)

object Ern {
  def toIdTypePair(ern: Ern): (UnitId, UnitType) =
    UnitId(ern.value) -> UnitType.Enterprise
}