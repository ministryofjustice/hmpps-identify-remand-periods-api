package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun LocalDate.isBeforeOrEqualTo(date: LocalDate): Boolean = this.isBefore(date) || this == date

fun LocalDate.isAfterOrEqualTo(date: LocalDate): Boolean = this.isAfter(date) || this == date

fun LocalDate.mojDisplayFormat(): String = this.format(
  DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK),
)
