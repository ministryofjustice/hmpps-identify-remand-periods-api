package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

data class RemandCalculation(
  val prisonerId: String,
  val charges: List<ChargeAndEvents>,
)
