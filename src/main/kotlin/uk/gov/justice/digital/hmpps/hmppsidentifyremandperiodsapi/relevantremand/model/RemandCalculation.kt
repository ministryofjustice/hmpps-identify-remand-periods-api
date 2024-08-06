package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

data class RemandCalculation(
  val prisonerId: String,
  val chargesAndEvents: List<ChargeAndEvents>,
  val chargeIdsWithActiveSentence: List<Long> = emptyList(),
  val issuesWithLegacyData: List<LegacyDataProblem> = emptyList(),
  val includeCalculationInResult: Boolean = false,
) {
  val charges: Map<Long, Charge> = chargesAndEvents.map { it.charge }.associateBy { it.chargeId }
}
