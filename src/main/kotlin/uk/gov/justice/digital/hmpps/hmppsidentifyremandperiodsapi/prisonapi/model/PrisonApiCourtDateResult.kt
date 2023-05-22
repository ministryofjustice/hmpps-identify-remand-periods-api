package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.prisonapi.model

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PrisonApiCourtDateResult(
  val id: Long,
  val date: LocalDate,
  val resultCode: String?,
  val resultDescription: String?,
  val resultDispositionCode: String?,
  val charge: PrisonApiCharge,
  val bookingId: Long,
)
