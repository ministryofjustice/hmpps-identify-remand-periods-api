package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDate

data class CalculationDetail(
  val releaseDate: LocalDate,
  val calculationIds: List<Long> = emptyList(),
  val unusedDeductions: Long? = null,
)
