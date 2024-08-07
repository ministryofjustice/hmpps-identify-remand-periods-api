package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

interface Period {
  val from: LocalDate
  val to: LocalDate

  val days: Long get() {
    return ChronoUnit.DAYS.between(from, to) + 1
  }
  fun overlapsEndInclusive(date: LocalDate): Boolean {
    return date.isAfter(from) && (date.isBefore(to) || date == to)
  }
  fun overlapsStartInclusive(date: LocalDate): Boolean {
    return (date.isAfter(from) || date == from) && date.isBefore(to)
  }

  fun overlaps(period: Period): Boolean {
    return overlapsStartInclusive(period.from) || overlapsEndInclusive(period.to) || datesSame(period)
  }

  fun datesSame(period: Period): Boolean {
    return from == period.from && to == period.to
  }

  fun engulfs(period: Period): Boolean {
    return (from.isBefore(period.from) || from == period.from) && (to.isAfter(period.to) || to == period.to)
  }
}
