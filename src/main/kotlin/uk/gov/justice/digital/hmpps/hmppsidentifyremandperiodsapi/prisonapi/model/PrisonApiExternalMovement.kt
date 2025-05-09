package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class PrisonApiExternalMovement(
  val offenderNo: String,
  val createDateTime: LocalDateTime?,
  val movementType: String,
  val movementTypeDescription: String?,
  val directionCode: String,
  val movementDate: LocalDate,
  val movementTime: LocalTime?,
  val movementReason: String?,
  val movementReasonCode: String,
  val fromAgency: String?,
  val commentText: String?,
)
