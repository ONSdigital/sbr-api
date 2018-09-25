package repository.sbrctrl

import uk.gov.ons.sbr.models.{ Period, UnitId, UnitKey, UnitType }

private[sbrctrl] object EditAdminDataPath {
  def apply(unitKey: UnitKey): String =
    s"v1/periods/${Period.asString(unitKey.period)}/types/${UnitType.toAcronym(unitKey.unitType)}/units/${unitKey.unitId.value}"
}