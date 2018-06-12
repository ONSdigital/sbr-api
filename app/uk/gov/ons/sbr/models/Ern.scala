package uk.gov.ons.sbr.models

case class Ern(value: String)

object Ern {
  def toIdTypePair(ern: Ern): (UnitId, UnitType) =
    asUnitId(ern) -> UnitType.Enterprise

  def asUnitId(ern: Ern): UnitId =
    UnitId(ern.value)
}