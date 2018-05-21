package uk.gov.ons.sbr.models

case class VatRef(value: String)

object VatRef {
  def toIdTypePair(vatref: VatRef): (UnitId, UnitType) =
    asUnitId(vatref) -> UnitType.ValueAddedTax

  def asUnitId(vatRef: VatRef): UnitId =
    UnitId(vatRef.value)
}
