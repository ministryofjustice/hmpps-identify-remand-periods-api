package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Charge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeAndEvents
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeRemand
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDate
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDateType.CONTINUE
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDateType.START
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDateType.STOP
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RelatedCharge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation

@Service
class RemandClockService {

  fun getRemandedCharges(remandCalculation: RemandCalculation): List<ChargeRemand> {
    val charges = combineRelatedCharges(remandCalculation)
    return remandClock(charges)
  }

  private fun remandClock(remandCalculation: RemandCalculation): List<ChargeRemand> {
    val remand = mutableListOf<ChargeRemand>()
    remandCalculation.chargesAndEvents.forEach { chargeAndEvent ->
      if (hasAnyRemandEvent(chargeAndEvent.dates)) {
        var from: CourtDate? = null
        chargeAndEvent.dates.forEach {
          if (listOf(START, CONTINUE).contains(it.type) && from == null) {
            from = it
          }
          if (it.type == STOP && from != null) {
            remand.add(ChargeRemand(from!!.date, getToDate(it), CourtAppearance(from!!.date, from!!.description), CourtAppearance(it.date, it.description), chargeAndEvent.charge.chargeId, null))
            from = null
          }
        }
      }
    }
    return remand
  }

  private fun combineRelatedCharges(remandCalculation: RemandCalculation): RemandCalculation {
    val mapOfRelatedCharges = remandCalculation.chargesAndEvents.groupBy {
      RelatedCharge(
        it.charge.offenceDate,
        it.charge.offenceEndDate,
        it.charge.offence.code,
      )
    }
    return remandCalculation.copy(
      chargesAndEvents = mapOfRelatedCharges.map {
        ChargeAndEvents(pickMostAppropriateCharge(it.value), flattenCourtDates(it.value))
      },
    )
  }

  private fun pickMostAppropriateCharge(relatedCharges: List<ChargeAndEvents>): Charge {
    val chargesWithSentence = relatedCharges.filter { it.charge.sentenceSequence != null }
    if (chargesWithSentence.isEmpty()) {
      return relatedCharges.first().charge
    }
    val chargesWithSentenceAndActiveBooking = chargesWithSentence.filter { it.charge.isActiveBooking }
    if (chargesWithSentenceAndActiveBooking.isEmpty()) {
      return chargesWithSentence.first().charge
    }
    return chargesWithSentenceAndActiveBooking.first().charge
  }

  private fun flattenCourtDates(relatedCharges: List<ChargeAndEvents>) =
    relatedCharges.flatMap { it.dates }.sortedBy { it.date }

  private fun hasAnyRemandEvent(courtDates: List<CourtDate>) = courtDates.any { listOf(START, CONTINUE).contains(it.type) }

  private fun getToDate(courtDate: CourtDate) = if (courtDate.final && courtDate.isCustodial) courtDate.date.minusDays(1) else courtDate.date
}
