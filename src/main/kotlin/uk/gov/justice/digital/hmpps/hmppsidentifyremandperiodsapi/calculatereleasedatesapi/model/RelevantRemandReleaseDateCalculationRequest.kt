package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.calculatereleasedatesapi.model

import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Sentence
import java.time.LocalDate

data class RelevantRemandReleaseDateCalculationRequest(
  val relevantRemands: List<RelevantRemand>,
  val sentence: Sentence,
  val calculateAt: LocalDate,
)
