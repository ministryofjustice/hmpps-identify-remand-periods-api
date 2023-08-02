package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation
import java.time.LocalDate

data class TestExample(
  val remandCalculation: RemandCalculation,
  val sentences: List<Sentences> = listOf(),
)

data class Sentences(
  val bookingId: Long,
  val sentenceSequence: Int,
  val sentenceAt: LocalDate,
  val recallDate: LocalDate? = null,
  val calculations: List<Calculations>,
)

data class Calculations(
  val calculateAt: LocalDate,
  val release: LocalDate,
  val postRecallReleaseDate: LocalDate? = null,
)
