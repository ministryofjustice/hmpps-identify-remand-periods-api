package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CalculationData
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeAndEvents
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeRemand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDate
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDateType.STOP
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandPeriodWithNoStop
import java.time.LocalDate

@Service
class RemandClockService {

  fun remandClock(calculationData: CalculationData): RemandClockResult {
    val remand = mutableListOf<ChargeRemand>()
    val unclosedRemandDates = mutableListOf<RemandPeriodWithNoStop>()
    calculationData.chargeAndEvents.forEach { chargeAndEvent ->
      if (hasAnyRemandEvent(chargeAndEvent.dates)) {
        var from: CourtDate? = null
        chargeAndEvent.dates.sortedBy { it.date }.forEach {
          if (it.type.shouldStartRemand() && from == null && dateIsBeforeSentenceDateIfExists(it.date, chargeAndEvent)) {
            from = it
          }
          if (it.type == STOP && from != null) {
            remand.add(ChargeRemand(from!!.date, getToDate(it), CourtAppearance(from!!.date, from!!.description), CourtAppearance(it.date, it.description), listOf(chargeAndEvent.charge.chargeId), null, isInConclusive = chargeAndEvent.charge.isInConclusive()))
            from = null
          }
        }
        if (from != null) {
          unclosedRemandDates.add(RemandPeriodWithNoStop(chargeAndEvent.charge, from!!.date))
        }
      }
    }
    return RemandClockResult(remand, unclosedRemandDates)
  }

  private fun dateIsBeforeSentenceDateIfExists(date: LocalDate, chargeAndEvent: ChargeAndEvents): Boolean = chargeAndEvent.charge.sentenceDate == null || date.isBefore(chargeAndEvent.charge.sentenceDate)

  private fun hasAnyRemandEvent(courtDates: List<CourtDate>) = courtDates.any { it.type.shouldStartRemand() }

  private fun getToDate(courtDate: CourtDate) = if (courtDate.final && courtDate.isCustodial) courtDate.date.minusDays(1) else courtDate.date
}

data class RemandClockResult(
  val chargeRemand: List<ChargeRemand>,
  val unclosedRemandDates: List<RemandPeriodWithNoStop>,

)
