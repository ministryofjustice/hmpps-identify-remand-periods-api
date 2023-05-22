package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDate

data class ExistingRemand(
  val from: LocalDate,
  val to: LocalDate,
  val sentence: Int,
)
