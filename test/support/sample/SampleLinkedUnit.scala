package support.sample

import uk.gov.ons.sbr.models._

object SampleLinkedUnit {
  def forEnterprise(period: Period, ern: Ern): LinkedUnit =
    LinkedUnit(
      UnitId(ern.value),
      UnitType.Enterprise,
      period,
      parents = None,
      children = Some(Map(UnitId("987654321") -> UnitType.LocalUnit)),
      vars = SampleEnterprise.asJson(ern)
    )

  def forVat(period: Period, vatRef: VatRef): LinkedUnit =
    LinkedUnit(
      UnitId(vatRef.value),
      UnitType.ValueAddedTax,
      period,
      parents = Some(Map(UnitType.Enterprise -> UnitId("1234567890"))),
      children = None,
      vars = SampleVat.asJson(vatRef)
    )

  def forPaye(period: Period, payeRef: PayeRef): LinkedUnit =
    LinkedUnit(
      UnitId(payeRef.value),
      UnitType.PayAsYouEarn,
      period,
      parents = Some(Map(UnitType.Enterprise -> UnitId("1234567890"))),
      children = None,
      vars = SamplePaye.asJson(payeRef)
    )
}
