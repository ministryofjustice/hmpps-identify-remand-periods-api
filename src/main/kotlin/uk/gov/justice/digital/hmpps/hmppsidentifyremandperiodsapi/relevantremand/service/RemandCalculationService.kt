package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.UnsupportedCalculationException
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.CalculationData
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculation
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandCalculationRequestOptions
import uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.relevantremand.model.RemandResult

@Service
class RemandCalculationService(
  private val relatedChargeCombinationService: RelatedChargeCombinationService,
  private val userSelectedCombinationService: UserSelectedCombinationService,
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

    val calculationData = CalculationData(issuesWithLegacyData = remandCalculation.issuesWithLegacyData.toMutableList())

    calculationData.chargeAndEvents = relatedChargeCombinationService.combineRelatedCharges(remandCalculation)

    validateChargeService.validate(calculationData)

    calculationData.chargeRemand = remandClockService.remandClock(calculationData)

    userSelectedCombinationService.combineUserSelectedCharges(calculationData, options)

    calculationData.sentenceRemandResult = sentenceRemandService.extractSentenceRemand(remandCalculation, calculationData)
    val adjustments = remandAdjustmentService.getRemandedAdjustments(remandCalculation, calculationData)

    calculationData.chargeRemand = chargeRemandStatusService.setChargeRemandStatuses(calculationData, adjustments, remandCalculation)
    calculationData.chargeRemand = mergeChargeRemandService.mergeChargeRemand(calculationData, remandCalculation)

    val unsortedResult = RemandResult(
      charges = remandCalculation.charges,
      adjustments = adjustments,
      chargeRemand = calculationData.chargeRemand,
      intersectingSentences = calculationData.sentenceRemandResult!!.intersectingSentences,
      issuesWithLegacyData = calculationData.issuesWithLegacyData,
      remandCalculation = if (options.includeRemandCalculation) remandCalculation else null,
    )

    return resultSortingService.sort(unsortedResult)
  }
}
