package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.UnsupportedCalculationException
import java.time.LocalDate

data class ChargeRemand(
  override val from: LocalDate,
  override val to: LocalDate,
  val fromEvent: CourtAppearance,
  val toEvent: CourtAppearance,
  val chargeIds: List<Long>,
  val status: ChargeRemandStatus?,
) : Period {

  fun onlyChargeId(): Long {
    if (chargeIds.size > 1) {
      throw UnsupportedCalculationException("Unexpected remand has more than one charge")
    }
    return chargeIds[0]
  }
}
