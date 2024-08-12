package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model

import java.time.LocalDateTime

data class SentenceCalculationSummary(
  val bookingId: Long,
  val offenderSentCalculationId: Long,
  val calculationDate: LocalDateTime,
  val commentText: String? = null,
  val calculationReason: String? = null,
)
