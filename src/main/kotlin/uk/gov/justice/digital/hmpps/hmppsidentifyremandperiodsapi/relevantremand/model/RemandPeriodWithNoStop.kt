package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDate

data class RemandPeriodWithNoStop(
  val charge: Charge,
  val start: LocalDate,
)
