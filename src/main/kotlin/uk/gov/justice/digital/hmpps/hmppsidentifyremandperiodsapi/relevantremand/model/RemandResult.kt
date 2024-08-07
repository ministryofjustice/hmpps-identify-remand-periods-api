package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.AdjustmentDto

data class RemandResult(
  val adjustments: List<AdjustmentDto> = emptyList(),
  val chargeRemand: List<ChargeRemand> = emptyList(),
  val sentenceRemand: List<Remand> = emptyList(),
  val intersectingSentences: List<SentencePeriod> = emptyList(),
  val charges: Map<Long, Charge> = emptyMap(),
  val periodsServingSentenceUsingCRDS: List<SentencePeriod> = emptyList(),
  val issuesWithLegacyData: List<LegacyDataProblem> = emptyList(),
  // Source for calculation. Only included for testing
  val remandCalculation: RemandCalculation? = null,
)
