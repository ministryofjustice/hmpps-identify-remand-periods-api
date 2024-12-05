package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model

import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.util.isAfterOrEqualTo
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.util.isBeforeOrEqualTo
import java.time.LocalDate
import java.time.temporal.ChronoUnit

interface Period {
  val from: LocalDate
  val to: LocalDate

  val days: Long get() {
    return ChronoUnit.DAYS.between(from, to) + 1
  }
  fun overlapsEndInclusive(date: LocalDate): Boolean {
    return date.isAfter(from) && date.isBeforeOrEqualTo(to)
  }
  fun overlapsStartInclusive(date: LocalDate): Boolean {
    return date.isAfterOrEqualTo(from) && date.isBefore(to)
  }

  fun overlapsStartAndEndInclusive(date: LocalDate): Boolean {
    return date.isAfterOrEqualTo(from) && date.isBeforeOrEqualTo(to)
  }

  fun overlaps(period: Period): Boolean {
    return (period.from.isAfterOrEqualTo(from) && period.from.isBeforeOrEqualTo(to)) ||
      (period.to.isAfterOrEqualTo(from) && period.to.isBeforeOrEqualTo(to))
  }

  fun datesSame(period: Period): Boolean {
    return from == period.from && to == period.to
  }

  fun engulfs(period: Period): Boolean {
    return from.isBeforeOrEqualTo(period.from) && to.isAfterOrEqualTo(period.to)
  }
}
