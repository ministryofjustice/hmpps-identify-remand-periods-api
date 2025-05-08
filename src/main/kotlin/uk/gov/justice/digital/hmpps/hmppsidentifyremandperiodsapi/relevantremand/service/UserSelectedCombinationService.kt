package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CalculationData
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.ChargeAndEvents
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CourtDate
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandApplicableUserSelection
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculationRequestOptions

@Service
class UserSelectedCombinationService {

  fun combineUserSelectedCharges(calculationData: CalculationData, options: RemandCalculationRequestOptions) {
    if (options.userSelections.isNotEmpty()) {
      calculationData.chargeAndEvents = calculationData.chargeAndEvents
        .filter { options.userSelections.none { selection -> selection.chargeIdsToMakeApplicable.contains(it.charge.chargeId) } }
        .map {
          val matchingSelection = options.userSelections.find { selection -> selection.targetChargeId == it.charge.chargeId }
          if (matchingSelection != null) {
            it.copy(
              dates = combineDatesAndUserSelection(it, calculationData.chargeAndEvents, matchingSelection),
              userSelectedCharges = matchingSelection.chargeIdsToMakeApplicable,
            )
          } else {
            it
          }
        }

      calculationData.chargeRemand = calculationData.chargeRemand.map {
        val matchingSelection = options.userSelections.find { selection -> selection.chargeIdsToMakeApplicable.contains(it.onlyChargeId()) }
        if (matchingSelection != null) {
          it.copy(
            chargeIds = listOf(matchingSelection.targetChargeId),
            replacedCharges = listOf(it.onlyChargeId()),
          )
        } else {
          it
        }
      }
    }
  }

  private fun combineDatesAndUserSelection(it: ChargeAndEvents, chargesAndEvents: List<ChargeAndEvents>, matchingSelection: RemandApplicableUserSelection): List<CourtDate> = it.dates + chargesAndEvents.filter { charge -> matchingSelection.chargeIdsToMakeApplicable.contains(charge.charge.chargeId) }.flatMap { charge -> charge.dates }.distinct()
}
