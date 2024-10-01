package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.UnsupportedCalculationException
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculationRequestOptions
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandResult

@Service
class RemandCalculationService(
  private val chargeCombinationService: ChargeCombinationService,
  private val remandClockService: RemandClockService,
  private val sentenceRemandService: SentenceRemandService,
  private val remandAdjustmentService: RemandAdjustmentService,
  private val chargeRemandStatusService: ChargeRemandStatusService,
  private val resultSortingService: ResultSortingService,
  private val mergeChargeRemandService: MergeChargeRemandService,
  private val validateChargeService: ValidateChargeService,
) {
  fun calculate(remandCalculation: RemandCalculation, options: RemandCalculationRequestOptions): RemandResult {
    if (remandCalculation.chargesAndEvents.isEmpty()) {
      throw UnsupportedCalculationException("There are no charges to calculate")
    }

    val combinedChargesAndEvents = chargeCombinationService.combineRelatedCharges(remandCalculation, options)

    validateChargeService.validate(remandCalculation, combinedChargesAndEvents)

    var chargeRemand = remandClockService.remandClock(combinedChargesAndEvents)
    val sentenceRemandResult = sentenceRemandService.extractSentenceRemand(remandCalculation, combinedChargesAndEvents, chargeRemand)
    val adjustments = remandAdjustmentService.getRemandedAdjustments(remandCalculation, sentenceRemandResult, chargeRemand)

    chargeRemand = chargeRemandStatusService.setChargeRemandStatuses(chargeRemand, adjustments, sentenceRemandResult, remandCalculation)
    chargeRemand = mergeChargeRemandService.mergeChargeRemand(chargeRemand, remandCalculation)

    val unsortedResult = RemandResult(
      charges = remandCalculation.charges,
      adjustments = adjustments,
      chargeRemand = chargeRemand,
      intersectingSentences = sentenceRemandResult.intersectingSentences,
      issuesWithLegacyData = remandCalculation.issuesWithLegacyData,
      remandCalculation = if (options.includeRemandCalculation) remandCalculation else null,
    )

    return resultSortingService.sort(unsortedResult)
  }
}
