package unitref

import uk.gov.ons.sbr.models.UnitType.PayAsYouEarn
import uk.gov.ons.sbr.models.{ PayeRef, UnitId, UnitType }

object PayeUnitRef extends UnitRef[PayeRef] {
  override def fromString(value: String): PayeRef =
    PayeRef(value)

  override def toIdTypePair(ref: PayeRef): (UnitId, UnitType) =
    toUnitId(ref) -> PayAsYouEarn

  override def toUnitId(ref: PayeRef): UnitId =
    UnitId(ref.value)
}
