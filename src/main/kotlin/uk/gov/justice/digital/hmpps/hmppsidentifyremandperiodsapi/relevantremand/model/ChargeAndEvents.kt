package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

data class ChargeAndEvents(
  val charge: Charge,
  val dates: List<CourtDate>,
  val similarCharges: List<Long> = emptyList(),
  val userCombinedCharges: List<Long> = emptyList(),
)
