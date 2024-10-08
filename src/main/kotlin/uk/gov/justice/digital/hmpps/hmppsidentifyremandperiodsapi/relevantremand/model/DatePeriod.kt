package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDate

data class DatePeriod(
  override val from: LocalDate,
  override val to: LocalDate,
) : Period
