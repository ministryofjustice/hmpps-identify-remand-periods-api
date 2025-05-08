package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

data class AdjustmentDto(
  val id: UUID?,
  val bookingId: Long,
  val sentenceSequence: Int?,
  val person: String,
  val adjustmentType: String = "REMAND",
  val fromDate: LocalDate?,
  val toDate: LocalDate?,
  val remand: RemandDto?,
  val status: AdjustmentStatus,
  val days: Int? = null,
  val source: String? = null,
) {
  fun daysBetween(): Int = (ChronoUnit.DAYS.between(fromDate, toDate) + 1).toInt()
}
