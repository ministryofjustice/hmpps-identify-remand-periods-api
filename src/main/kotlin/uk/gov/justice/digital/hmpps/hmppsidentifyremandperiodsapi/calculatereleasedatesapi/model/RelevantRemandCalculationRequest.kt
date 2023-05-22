package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.calculatereleasedatesapi.model

import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Sentence

data class RelevantRemandCalculationRequest(
  val relevantRemands: List<RelevantRemand>,
  val sentence: Sentence,
)
