package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model.AdjustmentDto

data class RemandResult(
  val adjustments: List<AdjustmentDto>,
  val chargeRemand: List<ChargeRemand>,
  val sentenceRemand: List<Remand>,
  val intersectingSentences: List<SentencePeriod>,
  val intersectingSentencesUsingHistoricCalculation: List<SentencePeriod> = emptyList(),
  val issuesWithLegacyData: List<LegacyDataProblem> = emptyList(),
)
