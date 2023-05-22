package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDate

data class CourtDate(
  val date: LocalDate,
  val type: CourtDateType,
  val final: Boolean = false,
)
