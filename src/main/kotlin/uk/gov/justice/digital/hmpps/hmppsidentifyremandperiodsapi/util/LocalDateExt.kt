package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun LocalDate.isBeforeOrEqualTo(date: LocalDate): Boolean {
  return this.isBefore(date) || this == date
}

fun LocalDate.isAfterOrEqualTo(date: LocalDate): Boolean {
  return this.isAfter(date) || this == date
}

fun LocalDate.mojDisplayFormat(): String {
  return this.format(
    DateTimeFormatter.ofPattern("d MMM yyyy"),
  )
}
