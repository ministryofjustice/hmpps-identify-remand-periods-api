package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CalculationData
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeRemand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDate
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDateType.CONTINUE
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDateType.START
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDateType.STOP

@Service
class RemandClockService {

  fun remandClock(calculationData: CalculationData): List<ChargeRemand> {
    val remand = mutableListOf<ChargeRemand>()
    calculationData.chargeAndEvents.forEach { chargeAndEvent ->
      if (hasAnyRemandEvent(chargeAndEvent.dates)) {
        var from: CourtDate? = null
        chargeAndEvent.dates.sortedBy { it.date }.forEach {
          if (listOf(START, CONTINUE).contains(it.type) && from == null) {
            from = it
          }
          if (it.type == STOP && from != null) {
            remand.add(ChargeRemand(from!!.date, getToDate(it), CourtAppearance(from!!.date, from!!.description), CourtAppearance(it.date, it.description), listOf(chargeAndEvent.charge.chargeId), null))
            from = null
          }
        }
      }
    }
    return remand
  }

  private fun hasAnyRemandEvent(courtDates: List<CourtDate>) = courtDates.any { listOf(START, CONTINUE).contains(it.type) }

  private fun getToDate(courtDate: CourtDate) = if (courtDate.final && courtDate.isCustodial) courtDate.date.minusDays(1) else courtDate.date
}
