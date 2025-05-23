package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import com.fasterxml.jackson.annotation.JsonIgnore

data class RemandCalculation(
  val prisonerId: String,
  val chargesAndEvents: List<ChargeAndEvents>,
  val imprisonmentStatuses: List<ImprisonmentStatus> = emptyList(),
  val chargeIdsWithActiveSentence: List<Long> = emptyList(),
  val issuesWithLegacyData: List<LegacyDataProblem> = listOf(),
  val externalMovements: List<ExternalMovement> = listOf(),
  val includeCalculationInResult: Boolean = false,
) {
  @JsonIgnore
  val charges: Map<Long, Charge> = chargesAndEvents.map { it.charge }.associateBy { it.chargeId }
}
