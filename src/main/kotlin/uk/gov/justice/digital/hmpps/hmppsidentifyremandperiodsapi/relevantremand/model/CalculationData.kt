package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

/*
  A mutable class that contains the data that can be modified throughout the calculation.
 */
data class CalculationData(
  var chargeAndEvents: List<ChargeAndEvents> = emptyList(),
  val imprisonmentStatuses: List<ImprisonmentStatus> = emptyList(),
  var chargeRemand: List<ChargeRemand> = emptyList(),
  var sentenceRemandResult: SentenceRemandResult? = null,
  val issuesWithLegacyData: MutableList<LegacyDataProblem> = mutableListOf(),
)
