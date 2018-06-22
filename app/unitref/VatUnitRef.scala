package unitref

import uk.gov.ons.sbr.models.UnitType.ValueAddedTax
import uk.gov.ons.sbr.models.{ UnitId, UnitType, VatRef }

object VatUnitRef extends UnitRef[VatRef] {
  override def fromUnitId(unitId: UnitId): VatRef =
    VatRef(unitId.value)

  override def toIdTypePair(ref: VatRef): (UnitId, UnitType) =
    UnitId(ref.value) -> ValueAddedTax
}
