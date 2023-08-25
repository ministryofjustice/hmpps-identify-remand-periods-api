package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDate

data class CourtDate(
  val date: LocalDate,
  val type: CourtDateType,
  val description: String = "",
  val final: Boolean = false,
  val isRecallEvent: Boolean = false,
  val isCustodial: Boolean = true,
)
