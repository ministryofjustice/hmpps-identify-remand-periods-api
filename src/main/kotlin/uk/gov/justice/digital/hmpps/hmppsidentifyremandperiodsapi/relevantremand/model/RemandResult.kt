package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

data class RemandResult(
  val chargeRemand: List<ChargeRemand>,
  val sentenceRemand: List<Remand>,
  val intersectingSentences: List<SentencePeriod>,
  val issuesWithLegacyData: List<LegacyDataProblem> = emptyList(),
  val unusedDeductions: Int = 0,
)
