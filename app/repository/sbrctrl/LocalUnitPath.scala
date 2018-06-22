package repository.sbrctrl

import uk.gov.ons.sbr.models.{ Ern, Lurn, Period }

private[sbrctrl] object LocalUnitPath {
  def apply(period: Period, ern: Ern, lurn: Lurn): String =
    s"v1/enterprises/${ern.value}/periods/${Period.asString(period)}/localunits/${lurn.value}"
}
