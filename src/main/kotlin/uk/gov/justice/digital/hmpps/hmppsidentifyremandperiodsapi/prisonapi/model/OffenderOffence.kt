package uk.gov.justice.digital.hmpps.adjustments.api.model.prisonapi

import java.time.LocalDate

data class OffenderOffence(
  val offenderChargeId: Long,
  val offenceStartDate: LocalDate? = null,
  val offenceEndDate: LocalDate? = null,
  val offenceCode: String = "",
  val offenceDescription: String = "",
  var indicators: List<String> = listOf(),
)
