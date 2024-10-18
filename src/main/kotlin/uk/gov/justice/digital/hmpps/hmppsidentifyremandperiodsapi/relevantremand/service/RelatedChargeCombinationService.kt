package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Charge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeAndEvents
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RelatedCharge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation

@Service
class RelatedChargeCombinationService {

  fun combineRelatedCharges(remandCalculation: RemandCalculation): List<ChargeAndEvents> {
    val (chargesWithOffenceDate, chargesWithoutOffenceDate) = remandCalculation.chargesAndEvents.partition { it.charge.offenceDate != null }
    val mapOfRelatedCharges = chargesWithOffenceDate.groupBy {
      RelatedCharge(
        it.charge.offenceDate!!,
        it.charge.offenceEndDate,
        it.charge.offence.code,
      )
    }
    return mapOfRelatedCharges.map {
      if (it.value.size > 1) {
        ChargeAndEvents(
          pickMostAppropriateCharge(it.value),
          flattenCourtDates(it.value),
          relatedCharges = it.value.map { combinedEvent -> combinedEvent.charge.chargeId },
        )
      } else {
        it.value[0]
      }
    } + chargesWithoutOffenceDate
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
    relatedCharges.flatMap { it.dates }.distinct()
}
