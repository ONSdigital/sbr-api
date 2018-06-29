package unitref

import uk.gov.ons.sbr.models.UnitType.ValueAddedTax
import uk.gov.ons.sbr.models.{ UnitId, UnitType, VatRef }

object VatUnitRef extends UnitRef[VatRef] {
  override def fromString(value: String): VatRef =
    VatRef(value)

  override def toIdTypePair(ref: VatRef): (UnitId, UnitType) =
    toUnitId(ref) -> ValueAddedTax

  override def toUnitId(ref: VatRef): UnitId =
    UnitId(ref.value)
}
