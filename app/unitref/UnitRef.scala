package unitref

import uk.gov.ons.sbr.models.{ UnitId, UnitType }

trait UnitRef[T] {
  def fromUnitId(unitId: UnitId): T
  def toIdTypePair(ref: T): (UnitId, UnitType)

  final def toUnitId(ref: T): UnitId =
    toIdTypePair(ref)._1
}
