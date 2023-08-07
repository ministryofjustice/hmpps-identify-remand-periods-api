package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model

import java.time.LocalDate
import java.util.UUID

data class AdjustmentDto(
  val id: UUID?,
  val bookingId: Long,
  val sentenceSequence: Int?,
  val person: String,
  val adjustmentType: String = "REMAND",
  val toDate: LocalDate?,
  val fromDate: LocalDate?,
)