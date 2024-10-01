package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.Charge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeAndEvents
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDate
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RelatedCharge
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandApplicableUserSelection
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculationRequestOptions

@Service
class ChargeCombinationService {

  fun combineRelatedCharges(remandCalculation: RemandCalculation, options: RemandCalculationRequestOptions): List<ChargeAndEvents> {
    val mapOfRelatedCharges = remandCalculation.chargesAndEvents.groupBy {
      RelatedCharge(
        it.charge.offenceDate,
        it.charge.offenceEndDate,
        it.charge.offence.code,
      )
    }
    val combined = mapOfRelatedCharges.map {
      if (it.value.size > 1) {
        ChargeAndEvents(
          pickMostAppropriateCharge(it.value),
          flattenCourtDates(it.value),
          similarCharges = it.value.map { combinedEvent -> combinedEvent.charge.chargeId },
        )
      } else {
        it.value[0]
      }
    }
    return combineUserSelectedCharges(combined, options)
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

  private fun combineUserSelectedCharges(chargesAndEvents: List<ChargeAndEvents>, options: RemandCalculationRequestOptions): List<ChargeAndEvents> {
    if (options.userSelections.isNotEmpty()) {
      return chargesAndEvents
        .filter { options.userSelections.none { selection -> selection.chargeIdsToMakeApplicable.contains(it.charge.chargeId) } }
        .map {
          val matchingSelection = options.userSelections.find { selection -> selection.targetChargeId == it.charge.chargeId }
          if (matchingSelection != null) {
            it.copy(
              dates = combineDatesAndUserSelection(it, chargesAndEvents, matchingSelection),
              userCombinedCharges = matchingSelection.chargeIdsToMakeApplicable,
            )
          } else {
            it
          }
        }
    }
    return chargesAndEvents
  }

  private fun combineDatesAndUserSelection(it: ChargeAndEvents, chargesAndEvents: List<ChargeAndEvents>, matchingSelection: RemandApplicableUserSelection): List<CourtDate> {
    return it.dates + chargesAndEvents.filter { charge -> matchingSelection.chargeIdsToMakeApplicable.contains(charge.charge.chargeId) }.flatMap { charge -> charge.dates }.distinct()
  }
}
