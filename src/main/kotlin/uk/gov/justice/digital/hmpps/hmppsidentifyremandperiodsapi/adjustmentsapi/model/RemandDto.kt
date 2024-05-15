package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.adjustmentsapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "The details of remand adjustment")
data class RemandDto(
  @Schema(description = "The id of the charges this remand applies to")
  val chargeId: List<Long>,
)
