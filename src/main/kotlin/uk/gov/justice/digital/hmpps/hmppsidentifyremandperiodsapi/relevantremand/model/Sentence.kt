package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDate

data class Sentence(
  val sequence: Int,
  val sentenceDate: LocalDate,
  val bookingId: Long,
)
