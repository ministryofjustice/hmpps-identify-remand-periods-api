package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

data class RemandResult(
  val chargeRemand: List<Remand>,
  val sentenceRemand: List<Remand>,
  val intersectingSentences: List<SentencePeriod>,
)
