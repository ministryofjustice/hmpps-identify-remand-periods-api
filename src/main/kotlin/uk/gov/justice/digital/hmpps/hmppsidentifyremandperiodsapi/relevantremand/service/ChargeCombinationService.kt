package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Charge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeAndEvents
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RelatedCharge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation

@Service
class ChargeCombinationService {

  fun combineRelatedCharges(remandCalculation: RemandCalculation): RemandCalculation {
    val mapOfRelatedCharges = remandCalculation.chargesAndEvents.groupBy {
      RelatedCharge(
        it.charge.offenceDate,
        it.charge.offenceEndDate,
        it.charge.offence.code,
      )
    }
    return remandCalculation.copy(
      chargesAndEvents = mapOfRelatedCharges.map {
        ChargeAndEvents(pickMostAppropriateCharge(it.value), flattenCourtDates(it.value), it.value.map { it.charge })
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
}
