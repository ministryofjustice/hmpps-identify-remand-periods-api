package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDate

data class CourtAppearance(
  val date: LocalDate,
  val description: String,
)
