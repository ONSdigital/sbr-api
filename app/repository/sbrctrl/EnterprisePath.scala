package repository.sbrctrl

import uk.gov.ons.sbr.models.{ Ern, Period }

private[sbrctrl] object EnterprisePath {
  def apply(period: Period, ern: Ern): String =
    s"v1/periods/${Period.asString(period)}/enterprises/${ern.value}"
}