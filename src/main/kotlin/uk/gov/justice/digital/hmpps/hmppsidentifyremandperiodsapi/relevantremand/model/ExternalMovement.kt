package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDate

data class ExternalMovement(
  val date: LocalDate,
  val release: Boolean,
)
