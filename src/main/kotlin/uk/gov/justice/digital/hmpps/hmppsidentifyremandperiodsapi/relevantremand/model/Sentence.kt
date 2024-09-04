package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDate

data class Sentence(
  val sequence: Int,
  val sentenceDate: LocalDate,
  val recallDates: List<LocalDate> = emptyList(),
  val bookingId: Long,
)
