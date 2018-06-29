package unitref

import uk.gov.ons.sbr.models.{ UnitId, UnitType }

trait UnitRef[T] {
  def fromString(value: String): T

  def toIdTypePair(ref: T): (UnitId, UnitType)
  def toUnitId(ref: T): UnitId
}
