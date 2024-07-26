package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

data class SentenceRemandResult(
  val sentenceRemand: List<Remand>,
  val intersectingSentences: List<SentencePeriod>,
  val periodsServingSentenceUsingCRDS: List<SentencePeriod> = emptyList(),
)
